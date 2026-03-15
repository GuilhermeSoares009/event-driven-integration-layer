package com.eventdriven.integrationlayer.processing;

import com.eventdriven.integrationlayer.inbox.InboxEvent;
import org.springframework.stereotype.Component;

@Component
public class NoopInboxEventHandler implements InboxEventHandler {

    @Override
    public void handle(InboxEvent event) {
    }
}
