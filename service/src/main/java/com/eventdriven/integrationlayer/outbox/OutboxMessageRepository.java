package com.eventdriven.integrationlayer.outbox;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

    List<OutboxMessage> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    List<OutboxMessage> findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
        String status,
        OffsetDateTime nextRetryAt,
        Pageable pageable
    );

    @Modifying
    @Transactional
    @Query("update OutboxMessage m set m.status = :toStatus where m.id = :id and m.status = :fromStatus")
    int transitionStatus(@Param("id") Long id, @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);
}
