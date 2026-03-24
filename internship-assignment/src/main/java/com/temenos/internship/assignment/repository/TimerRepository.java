package com.temenos.internship.assignment.repository;

import com.temenos.internship.assignment.model.TimerEntity;
import com.temenos.internship.assignment.model.TimerStatus;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TimerRepository extends ReactiveCrudRepository<TimerEntity, UUID> {

    @Modifying
    @Query("""
        UPDATE timer
        SET status = 'SCHEDULED'
        WHERE (status = 'STORED' AND created + (delay * 1000) - :now < :threshold)
        OR (status = 'FAILED' AND fail_count < :maxRetries)
        RETURNING timer_id, delay, created, status, fail_count
        """)
    Flux<TimerEntity> claimTimersForScheduling(long now, long threshold, int maxRetries);

    /**
     * Updates the status of a timer by ID.
     *
     * @param timerId the timer to update
     * @param status the new status
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE timer SET status = :status WHERE timer_id = :timerId")
    Mono<Integer> updateStatus(UUID timerId, TimerStatus status);

    /**
     * Updates the status and increments fail count atomically.
     *
     * @param timerId the timer to update
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE timer SET status = 'FAILED', fail_count = fail_count + 1 WHERE timer_id = :timerId")
    Mono<Integer> markAsFailed(UUID timerId);
}
