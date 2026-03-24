package com.temenos.internship.assignment.service;

import com.temenos.internship.assignment.config.TimerProperties;
import com.temenos.internship.assignment.model.TimerStatus;
import com.temenos.internship.assignment.repository.TimerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimerConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(TimerConsumerService.class);

    private final TimerQueueService timerQueueService;
    private final TimerRepository timerRepository;
    @PostConstruct
    public void startConsuming() {
        Thread consumerThread = new Thread(this::consumeLoop);
        consumerThread.setName("timer-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
        logger.debug("Timer consumer started");
    }
    private void consumeLoop() {
        RBlockingQueue<String> blockingQueue = timerQueueService.getBlockingQueue();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                String timerId = blockingQueue.take();
                processTimer(timerId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("Consumer thread interrupted");
            }
        }
    }
    private void processTimer(String timerId) {
        UUID id = UUID.fromString(timerId);

        timerRepository.updateStatus(id, TimerStatus.PROCESSING)
                .flatMap(rows -> {
                    logger.debug("Timer {} set to PROCESSING", timerId);
                    return doWork(timerId)
                            .then(timerRepository.updateStatus(id, TimerStatus.COMPLETED)) // chain after work
                            .doOnSuccess(r -> logger.debug("Timer {} COMPLETED", timerId));
                })
                .onErrorResume(error -> {
                    logger.error("Timer {} failed: {}", timerId, error.getMessage());
                    return timerRepository.markAsFailed(id);
                })
                .subscribe(); // still async
    }

    private Mono<Void> doWork(String timerId) {
        logger.debug("Executing work for timer {}", timerId);
        // simulate work
        return Mono.delay(Duration.ofSeconds(1)).then();
    }
}
