package com.eventdriven.integrationlayer.ops;

import com.eventdriven.integrationlayer.inbox.InboxEvent;
import com.eventdriven.integrationlayer.inbox.InboxEventRepository;
import com.eventdriven.integrationlayer.outbox.OutboxMessageRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops")
public class OpsController {

    private static final List<String> REPLAY_STATUSES = List.of(InboxEvent.STATUS_FAILED, InboxEvent.STATUS_DEAD);

    private final InboxEventRepository inboxEventRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final Environment environment;

    public OpsController(
        InboxEventRepository inboxEventRepository,
        OutboxMessageRepository outboxMessageRepository,
        Environment environment
    ) {
        this.inboxEventRepository = inboxEventRepository;
        this.outboxMessageRepository = outboxMessageRepository;
        this.environment = environment;
    }

    @PostMapping("/inbox/{id}/replay")
    public ResponseEntity<Map<String, Object>> replayInboxEvent(@PathVariable long id) {
        InboxEvent event = inboxEventRepository.findById(id).orElse(null);
        if (event == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "inbox_event_not_found"));
        }

        if (!REPLAY_STATUSES.contains(event.getStatus())) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", "inbox_event_not_eligible"));
        }

        event.setStatus(InboxEvent.STATUS_RECEIVED);
        event.setNextRetryAt(null);
        inboxEventRepository.save(event);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
            "status", "accepted",
            "replayed", true,
            "inbox_event_id", event.getId()
        ));
    }

    @PostMapping("/inbox/replay-batch")
    public ResponseEntity<Map<String, Object>> replayInboxBatch(
        @RequestParam(required = false) String provider,
        @RequestParam(required = false) String topic,
        @RequestParam(required = false) Integer limit
    ) {
        int safeLimit = sanitizeLimit(limit);
        Pageable page = PageRequest.of(0, safeLimit);

        List<InboxEvent> events = findReplayCandidates(provider, topic, page);
        for (InboxEvent event : events) {
            event.setStatus(InboxEvent.STATUS_RECEIVED);
            event.setNextRetryAt(null);
        }

        inboxEventRepository.saveAll(events);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
            "status", "accepted",
            "replayed", events.size()
        ));
    }

    @DeleteMapping("/inbox/prune")
    public ResponseEntity<Map<String, Object>> pruneInbox(@RequestParam(required = false) Integer days) {
        int retentionDays = resolveDays(days, "integration.retention.inbox-days", 30);
        if (retentionDays <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_days"));
        }

        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(retentionDays);
        long deleted = inboxEventRepository.deleteByReceivedAtBefore(cutoff);

        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "deleted", deleted,
            "cutoff", cutoff.toString()
        ));
    }

    @DeleteMapping("/outbox/prune")
    public ResponseEntity<Map<String, Object>> pruneOutbox(@RequestParam(required = false) Integer days) {
        int retentionDays = resolveDays(days, "integration.retention.outbox-days", 30);
        if (retentionDays <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_days"));
        }

        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(retentionDays);
        long deleted = outboxMessageRepository.deleteByCreatedAtBefore(cutoff);

        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "deleted", deleted,
            "cutoff", cutoff.toString()
        ));
    }

    private List<InboxEvent> findReplayCandidates(String provider, String topic, Pageable page) {
        boolean hasProvider = provider != null && !provider.isBlank();
        boolean hasTopic = topic != null && !topic.isBlank();

        if (hasProvider && hasTopic) {
            return inboxEventRepository.findByStatusInAndProviderAndTopicOrderByReceivedAtAsc(
                REPLAY_STATUSES,
                provider,
                topic,
                page
            );
        }

        if (hasProvider) {
            return inboxEventRepository.findByStatusInAndProviderOrderByReceivedAtAsc(
                REPLAY_STATUSES,
                provider,
                page
            );
        }

        if (hasTopic) {
            return inboxEventRepository.findByStatusInAndTopicOrderByReceivedAtAsc(
                REPLAY_STATUSES,
                topic,
                page
            );
        }

        return inboxEventRepository.findByStatusInOrderByReceivedAtAsc(REPLAY_STATUSES, page);
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 100;
        }
        return Math.min(limit, 1000);
    }

    private int resolveDays(Integer providedDays, String propertyKey, int defaultValue) {
        if (providedDays != null) {
            return providedDays;
        }

        Integer configured = environment.getProperty(propertyKey, Integer.class, defaultValue);
        return configured == null ? defaultValue : configured;
    }
}
