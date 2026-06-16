/**
 * 
 */
package com.server.realsync.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.realsync.entity.MessageQueue;
import com.server.realsync.entity.QueueStatus;
import com.server.realsync.repo.MessageQueueRepository;

/**
 * 
 */

@Service
public class MessageQueueService {

    private final MessageQueueRepository repository;

    public MessageQueueService(MessageQueueRepository repository) {
        this.repository = repository;
    }

    public MessageQueue save(MessageQueue job) {
        return repository.save(job);
    }

    @Transactional
    public List<MessageQueue> fetchJobs(int batchSize) {

        List<MessageQueue> jobs =
                repository.fetchBatchForProcessing(batchSize);

        jobs.forEach(j -> j.setStatus(QueueStatus.SENDING));

        return jobs;
    }

    public void markDone(MessageQueue job) {

        job.setStatus(QueueStatus.DONE);
        repository.save(job);
    }

    public void markFailed(MessageQueue job, String error) {

        job.setStatus(QueueStatus.FAILED);
        job.setRetryCount(job.getRetryCount() + 1);

        repository.save(job);
    }

    public boolean exists(com.server.realsync.entity.EntityType entityType, Long entityEntryId) {
        return repository.existsByEntityTypeAndEntityEntryId(entityType, entityEntryId);
    }

    public java.util.Optional<MessageQueue> findByMessageId(String messageId) {
        return repository.findByMessageId(messageId);
    }
}