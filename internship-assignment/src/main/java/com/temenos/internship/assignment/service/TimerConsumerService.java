package com.temenos.internship.assignment.service;

import com.temenos.internship.assignment.model.Timer;
import com.temenos.internship.assignment.model.TimerEntity;
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
import org.springframework.web.reactive.function.client.WebClient;
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
    private final WebClient webClient;
    private final AtomicBoolean running = new AtomicBoolean(true);


    public TimerConsumerService(
            TimerQueueService timerQueueService,
            TimerRepository timerRepository,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
            WebClient webClient
    ) {
        this.timerQueueService = timerQueueService;
        this.timerRepository = timerRepository;
        this.taskExecutor = taskExecutor;
        this.webClient = webClient;
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
                .then(timerRepository.findById(id))
                .switchIfEmpty(Mono.error(
                        new IllegalStateException("Timer not found: " + timerId)))
                .flatMap(entity -> {
                    Timer timer = toModel(entity);

                    return doWork(timerId)
                            .then(sendCallback(timer))
                            .then(timerRepository.updateStatus(
                                    id, TimerStatus.COMPLETED))
                            .doOnSuccess(v ->
                                    logger.info(
                                            "Timer {} COMPLETED and callback sent",
                                            timerId
                                    ));
                })
                .onErrorResume(error -> {
                    logger.error("Timer {} failed", timerId, error);
                    return timerRepository.markAsFailed(id);
                })
                .subscribe();
    }

    private Mono<Void> sendCallback(Timer timer) {
        return webClient.post()
                .uri(timer.getCallbackUrl())
                .bodyValue(timer)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response ->
                        logger.info(
                                "Callback sent successfully for timer {} with status {}",
                                timer.getTimerId(),
                                response.getStatusCode()
                        )
                )
                .doOnError(error ->
                        logger.error(
                                "Callback failed for timer {}",
                                timer.getTimerId(),
                                error
                        )
                )
                .then();
    }



    private Mono<Void> doWork(String timerId) {
        logger.debug("Executing work for timer {}", timerId);
        return Mono.delay(Duration.ofSeconds(5)).then();
    }

    private Timer toModel(TimerEntity entity) {
        Timer timer = new Timer();
        timer.setTimerId(entity.getTimerId().toString());
        timer.setCreatedAt(entity.getCreated());
        timer.setDelay(entity.getDelay());
        timer.setStatus(TimerStatus.valueOf(entity.getStatus().name()));
        timer.setFailCount(entity.getFailCount());
        timer.setCallbackUrl(entity.getCallbackUrl());
        timer.setCsrfToken(entity.getCsrfToken());
        return timer;
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        logger.info("Timer consumer shutting down");
    }


}
