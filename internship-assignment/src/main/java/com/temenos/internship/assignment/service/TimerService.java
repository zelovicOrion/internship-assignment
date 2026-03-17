package com.temenos.internship.assignment.service;

import com.temenos.internship.assignment.model.Timer;
import com.temenos.internship.assignment.model.TimerEntity;
import com.temenos.internship.assignment.model.TimerRequest;
import com.temenos.internship.assignment.repository.TimerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimerService {
    private static final Logger logger = LoggerFactory.getLogger(TimerService.class);
    private final TimerRepository timerRepository;

    public Mono<Timer> createTimer(TimerRequest request) {
        logger.debug("Creating timer with delay: {}", request.getDelay());

        TimerEntity entity = new TimerEntity();
        entity.setTimerId(UUID.randomUUID());
        entity.setCreated(System.currentTimeMillis());
        entity.setDelay(request.getDelay());

        return timerRepository.save(entity)
                .map(this::toModel);
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

    private Timer toModel(TimerEntity entity) {
        Timer timer = new Timer();
        timer.setTimerId(entity.getTimerId().toString());
        timer.setCreatedAt(entity.getCreated());
        timer.setDelay(entity.getDelay());
        return timer;
    }

}
