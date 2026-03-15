package com.eventdriven.integrationlayer.ops;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventdriven.integrationlayer.inbox.InboxEvent;
import com.eventdriven.integrationlayer.inbox.InboxEventRepository;
import com.eventdriven.integrationlayer.outbox.OutboxMessageRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OpsController.class)
class OpsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InboxEventRepository inboxEventRepository;

    @MockBean
    private OutboxMessageRepository outboxMessageRepository;

    @Test
    void shouldReturnNotFoundWhenReplayEventDoesNotExist() throws Exception {
        when(inboxEventRepository.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/ops/inbox/404/replay"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("inbox_event_not_found"));
    }

    @Test
    void shouldReplaySingleEventWhenEligible() throws Exception {
        InboxEvent event = new InboxEvent();
        event.setId(10L);
        event.setStatus(InboxEvent.STATUS_DEAD);

        when(inboxEventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(inboxEventRepository.save(any(InboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/ops/inbox/10/replay"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("accepted"))
            .andExpect(jsonPath("$.replayed").value(true))
            .andExpect(jsonPath("$.inbox_event_id").value(10));
    }

    @Test
    void shouldPruneInboxWithExplicitDays() throws Exception {
        when(inboxEventRepository.deleteByReceivedAtBefore(any())).thenReturn(2L);

        mockMvc.perform(delete("/api/v1/ops/inbox/prune?days=30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.deleted").value(2));
    }

    @Test
    void shouldPruneOutboxWithExplicitDays() throws Exception {
        when(outboxMessageRepository.deleteByCreatedAtBefore(any())).thenReturn(3L);

        mockMvc.perform(delete("/api/v1/ops/outbox/prune?days=30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.deleted").value(3));
    }

}
