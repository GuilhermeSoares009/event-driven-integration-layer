package com.eventdriven.integrationlayer.inbox;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface InboxEventRepository extends JpaRepository<InboxEvent, Long> {

    boolean existsByProviderAndExternalEventId(String provider, String externalEventId);

    boolean existsByProviderAndExternalEventIdIsNullAndPayloadHash(String provider, String payloadHash);

    List<InboxEvent> findByStatusOrderByReceivedAtAsc(String status, Pageable pageable);

    List<InboxEvent> findByStatusAndNextRetryAtLessThanEqualOrderByReceivedAtAsc(
        String status,
        OffsetDateTime nextRetryAt,
        Pageable pageable
    );

    List<InboxEvent> findByStatusInOrderByReceivedAtAsc(List<String> statuses, Pageable pageable);

    List<InboxEvent> findByStatusInAndProviderOrderByReceivedAtAsc(
        List<String> statuses,
        String provider,
        Pageable pageable
    );

    List<InboxEvent> findByStatusInAndTopicOrderByReceivedAtAsc(
        List<String> statuses,
        String topic,
        Pageable pageable
    );

    List<InboxEvent> findByStatusInAndProviderAndTopicOrderByReceivedAtAsc(
        List<String> statuses,
        String provider,
        String topic,
        Pageable pageable
    );

    @Transactional
    long deleteByReceivedAtBefore(OffsetDateTime cutoff);

    @Modifying
    @Transactional
    @Query("update InboxEvent e set e.status = :toStatus where e.id = :id and e.status = :fromStatus")
    int transitionStatus(@Param("id") Long id, @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);
}
