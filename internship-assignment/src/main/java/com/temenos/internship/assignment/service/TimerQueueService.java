package com.temenos.internship.assignment.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
public class TimerQueueService {
    private static final Logger logger = LoggerFactory.getLogger(TimerQueueService.class);
    private static final String QUEUE_NAME = "timers";

    private final RedissonClient redissonClient;

    public void scheduleTimer(String timerId, int delay, long createdAt) {
        long elapsedSeconds = (System.currentTimeMillis() - createdAt) / 1000;
        long remainingSeconds = Math.max(0, delay - elapsedSeconds);

        RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(QUEUE_NAME);
        RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);

        delayedQueue.offer(timerId, remainingSeconds, TimeUnit.SECONDS);

        logger.debug("Timer {} scheduled with {}s remaining", timerId, remainingSeconds);
    }

    public RBlockingQueue<String> getBlockingQueue() {
        return redissonClient.getBlockingQueue(QUEUE_NAME);
    }
}
