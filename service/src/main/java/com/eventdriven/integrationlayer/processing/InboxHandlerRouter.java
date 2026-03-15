package com.eventdriven.integrationlayer.processing;

import com.eventdriven.integrationlayer.inbox.InboxEvent;
import org.springframework.stereotype.Component;

@Component
public class InboxHandlerRouter {

    private final NoopInboxEventHandler noopInboxEventHandler;

    public InboxHandlerRouter(NoopInboxEventHandler noopInboxEventHandler) {
        this.noopInboxEventHandler = noopInboxEventHandler;
    }

    public InboxEventHandler resolve(InboxEvent event) {
        return noopInboxEventHandler;
    }
}
