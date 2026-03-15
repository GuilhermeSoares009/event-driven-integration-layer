package com.eventdriven.integrationlayer.processing;

import com.eventdriven.integrationlayer.inbox.InboxEvent;

public interface InboxEventHandler {

    void handle(InboxEvent event);
}
