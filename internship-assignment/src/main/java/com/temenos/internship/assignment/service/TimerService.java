package com.temenos.internship.assignment.service;

import com.temenos.internship.assignment.config.TimerProperties;
import com.temenos.internship.assignment.model.Timer;
import com.temenos.internship.assignment.model.TimerEntity;
import com.temenos.internship.assignment.model.TimerStatus;
import com.temenos.internship.assignment.repository.TimerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimerService {
    private static final Logger logger = LoggerFactory.getLogger(TimerService.class);
    private final TimerRepository timerRepository;
    private final TimerProperties timerProperties;
    private final TimerQueueService timerQueueService;
    private static final String REQUEST_STREAM = "timer-requests";
    private static final String GROUP_NAME = "timer-service-group";

    private final String consumerName =
            Optional.ofNullable(System.getenv("HOSTNAME"))
                    .orElse(UUID.randomUUID().toString());

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void startStreamListener() {
        createConsumerGroupIfNeeded()
                .thenMany(readStream())
                .flatMap(this::processStreamMessage)
                .onErrorContinue((error, value) ->
                        logger.error("Error processing Redis stream message", error))
                .subscribe();

        logger.info("Redis stream listener started: {}", consumerName);
    }

    private Mono<Void> createConsumerGroupIfNeeded() {
        return redisTemplate.opsForStream()
                .createGroup(REQUEST_STREAM, ReadOffset.latest(), GROUP_NAME)
                .doOnSuccess(v ->
                        logger.info("Consumer group '{}' created", GROUP_NAME))
                .onErrorResume(e -> {
                    logger.debug("Consumer group already exists");
                    return Mono.empty();
                })
                .then();
    }

    private Flux<MapRecord<String, Object, Object>> readStream() {
        return redisTemplate.opsForStream()
                .read(
                        Consumer.from(GROUP_NAME, consumerName),
                        StreamReadOptions.empty()
                                .count(10)
                                .block(Duration.ofSeconds(2)),
                        StreamOffset.create(
                                REQUEST_STREAM,
                                ReadOffset.lastConsumed()
                        )
                )
                .repeat();
    }

    private Mono<Void> processStreamMessage(
            MapRecord<String, Object, Object> message) {

        Map<Object, Object> values = message.getValue();

        String timerId = (String) values.get("timerId");
        int delay = Integer.parseInt((String) values.get("delay"));
        long createdAt = Long.parseLong((String) values.get("createdAt"));

        logger.debug("Processing timer {}", timerId);

        return saveAndSchedule(timerId, delay, createdAt)
                .then(redisTemplate.opsForStream()
                        .acknowledge(
                                REQUEST_STREAM,
                                GROUP_NAME,
                                message.getId()))
                .then();
    }
    private Mono<Void> saveAndSchedule(
            String timerId,
            int delay,
            long createdAt) {

        TimerEntity entity = buildEntity(
                UUID.fromString(timerId),
                delay,
                createdAt
        );

        if (isShortTimer(delay)) {
            entity.setStatus(TimerStatus.SCHEDULED);
            return timerRepository.save(entity)
                    .doOnSuccess(saved -> {
                        timerQueueService.scheduleTimer(
                                timerId, delay, createdAt);
                        logger.debug(
                                "Short timer {} scheduled", timerId);
                    })
                    .then();
        }

        entity.setStatus(TimerStatus.STORED);
        return timerRepository.save(entity)
                .doOnSuccess(saved ->
                        logger.debug(
                                "Long timer {} stored", timerId))
                .then();
    }

    public Flux<Timer> getAllTimers() {
        logger.debug("Fetching all timers");
        return timerRepository.findAll()
                .map(this::toModel);
    }

    public Mono<Timer> getTimerById(String timerId) {
        logger.debug("Fetching timer with id: {}", timerId);
        return timerRepository.findById(UUID.fromString(timerId))
                .map(this::toModel);
    }

    public Mono<Void> deleteTimer(String timerId) {
        logger.debug("Deleting timer with id: {}", timerId);
        return timerRepository.deleteById(UUID.fromString(timerId));
    }

    private TimerEntity buildEntity(UUID timerId, int delay, long createdAt) {
        TimerEntity entity = new TimerEntity();
        entity.setTimerId(timerId);
        entity.setDelay(delay);
        entity.setCreated(createdAt);
        entity.setFailCount(0);
        return entity;
    }
    private boolean isShortTimer(int delay) {
        return delay < timerProperties.getShortTimerThreshold();
    }

    private Timer toModel(TimerEntity entity) {
        Timer timer = new Timer();
        timer.setTimerId(entity.getTimerId().toString());
        timer.setCreatedAt(entity.getCreated());
        timer.setDelay(entity.getDelay());
        timer.setStatus(TimerStatus.valueOf(entity.getStatus().name()));
        timer.setFailCount(entity.getFailCount());
        return timer;
    }

}
