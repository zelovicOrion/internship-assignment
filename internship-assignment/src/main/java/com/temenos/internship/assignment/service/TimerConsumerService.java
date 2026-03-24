package com.temenos.internship.assignment.service;

import com.temenos.internship.assignment.model.TimerStatus;
import com.temenos.internship.assignment.repository.TimerRepository;
import jakarta.annotation.PreDestroy;
import org.redisson.api.RBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.core.task.TaskExecutor;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TimerConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(TimerConsumerService.class);

    private final TimerQueueService timerQueueService;
    private final TimerRepository timerRepository;

    private final TaskExecutor taskExecutor;

    private final AtomicBoolean running = new AtomicBoolean(true);


    public TimerConsumerService(
            TimerQueueService timerQueueService,
            TimerRepository timerRepository,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor
    ) {
        this.timerQueueService = timerQueueService;
        this.timerRepository = timerRepository;
        this.taskExecutor = taskExecutor;
    }


    @EventListener(ApplicationReadyEvent.class)
    public void startConsuming() {
        taskExecutor.execute(this::consumeLoop);
        logger.info("Timer consumer started");
    }
    private void consumeLoop() {
        RBlockingQueue<String> blockingQueue = timerQueueService.getBlockingQueue();
        while (running.get()) {
            try {
                String timerId = blockingQueue.take();
                processTimer(timerId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Timer consumer interrupted, stopping");
                break;
            } catch (Exception e) {
                logger.error("Unexpected error in consumer loop", e);
            }
        }

    }

    private void processTimer(String timerId) {
        UUID id = UUID.fromString(timerId);

        timerRepository.updateStatus(id, TimerStatus.PROCESSING)
                .flatMap(rows -> {
                    logger.debug("Timer {} set to PROCESSING", timerId);
                    return doWork(timerId)
                            .then(timerRepository.updateStatus(
                                    id, TimerStatus.COMPLETED))
                            .doOnSuccess(r ->
                                    logger.debug("Timer {} COMPLETED", timerId));
                })
                .onErrorResume(error -> {
                    logger.error("Timer {} failed", timerId, error);
                    return timerRepository.markAsFailed(id);
                })
                .subscribe();
    }



    private Mono<Void> doWork(String timerId) {
        logger.debug("Executing work for timer {}", timerId);
        return Mono.delay(Duration.ofSeconds(5)).then();
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        logger.info("Timer consumer shutting down");
    }


}
