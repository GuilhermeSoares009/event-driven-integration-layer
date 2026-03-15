package com.eventdriven.integrationlayer.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxEventRepository extends JpaRepository<InboxEvent, Long> {

    boolean existsByProviderAndExternalEventId(String provider, String externalEventId);

    boolean existsByProviderAndExternalEventIdIsNullAndPayloadHash(String provider, String payloadHash);
}
