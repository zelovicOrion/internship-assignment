package com.temenos.internship.assignment.controller;

import com.temenos.internship.assignment.service.TimerService;
import com.temenos.internship.assignment.service.stream.TimerRequestBuilder;
import com.temenos.internship.assignment.service.stream.TimerStreamRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;

import com.temenos.internship.assignment.api.TimerApi;
import com.temenos.internship.assignment.model.Timer;
import com.temenos.internship.assignment.model.TimerRequest;

@RestController
@RequiredArgsConstructor
public class TimerController implements TimerApi {

    private static final Logger logger = LoggerFactory.getLogger(TimerController.class);
    private final TimerService timerService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String REQUEST_STREAM = "timer-requests";
    private final TimerRequestBuilder timerRequestBuilder;
    @GetMapping("/test")
    Mono<ResponseEntity<String>> test() {
        logger.debug("Received request on /test endpoint");
        return Mono.just(ResponseEntity.ok("test ok"));
    }

    @Override
    public Mono<ResponseEntity<Timer>> createTimer(
            Mono<TimerRequest> timerRequest,
            ServerWebExchange exchange) {

        return timerRequest.flatMap(request -> {
            TimerStreamRequest streamRequest =
                    timerRequestBuilder.build(request);

            logger.debug(
                    "Publishing timer {} to request stream",
                    streamRequest.timerId()
            );

            return redisTemplate.opsForStream()
                    .add(REQUEST_STREAM, streamRequest.toStreamMap())
                    .map(recordId -> {
                        Timer timer = new Timer();
                        timer.setTimerId(streamRequest.timerId());
                        timer.setDelay(streamRequest.delay());
                        timer.setCreatedAt(streamRequest.createdAt());
                        timer.setCallbackUrl(streamRequest.callbackUrl());
                        timer.setCsrfToken(streamRequest.csrfToken());
                        return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(timer);
                    });
        });
    }

    @Override
    public Mono<ResponseEntity<Flux<Timer>>> getAllTimers(
            ServerWebExchange exchange) {
        logger.debug("GET /api/timers - getAllTimers called");
        return Mono.just(ResponseEntity.ok(timerService.getAllTimers()));
    }

    @Override
    public Mono<ResponseEntity<Timer>> getTimerById(
            String timerId,
            ServerWebExchange exchange) {
        logger.debug("GET /api/timers/{} - getTimerById called", timerId);
        return timerService.getTimerById(timerId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteTimer(
            String timerId,
            ServerWebExchange exchange) {
        logger.debug("DELETE /api/timers/{} - deleteTimer called", timerId);
        return timerService.deleteTimer(timerId)
                .map(ResponseEntity::ok);
    }
}
