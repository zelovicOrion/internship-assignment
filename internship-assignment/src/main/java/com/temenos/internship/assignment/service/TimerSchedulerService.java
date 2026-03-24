package com.temenos.internship.assignment.service;

import com.temenos.internship.assignment.config.TimerProperties;
import com.temenos.internship.assignment.repository.TimerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimerSchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(TimerSchedulerService.class);

    private final TimerRepository timerRepository;
    private final TimerQueueService timerQueueService;
    private final TimerProperties timerProperties;

    @Scheduled(fixedDelayString = "${timer.scheduler-interval-ms:1800000}")
    public void scheduleEligibleTimers() {
        logger.debug("Scheduler running - checking for eligible timers");

        long now = System.currentTimeMillis();
        long thresholdMs = (long) timerProperties.getShortTimerThreshold() * 1000;

        timerRepository.claimTimersForScheduling(now, thresholdMs, timerProperties.getMaxRetries())
                .doOnNext(entity -> {
                    logger.debug("Claimed timer {} for scheduling", entity.getTimerId());
                    timerQueueService.scheduleTimer(
                            entity.getTimerId().toString(),
                            entity.getDelay(),
                            entity.getCreated()
                    );
                })
                .doOnComplete(() -> logger.debug("Scheduler finished"))
                .subscribe();
    }
}
