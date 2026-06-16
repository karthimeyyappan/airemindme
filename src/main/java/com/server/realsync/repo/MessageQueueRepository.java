/**
 * 
 */
package com.server.realsync.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.server.realsync.entity.MessageQueue;

/**
 * 
 */

public interface MessageQueueRepository extends JpaRepository<MessageQueue, Long> {

    @Query(value = """
    SELECT *
    FROM message_queue
    WHERE status='PENDING'
    ORDER BY priority ASC, id ASC
    LIMIT :batchSize
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<MessageQueue> fetchBatchForProcessing(@Param("batchSize") int batchSize);

    boolean existsByEntityTypeAndEntityEntryId(com.server.realsync.entity.EntityType entityType, Long entityEntryId);

    java.util.Optional<MessageQueue> findByMessageId(String messageId);
}