package com.loopers.application.waitingroom;

import com.loopers.domain.waitingroom.WaitingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WaitingRoomScheduler {

    private final WaitingRoomService waitingRoomService;

    @Scheduled(fixedDelay = 1000)
    public void run() {
        waitingRoomService.processQueue();
    }
}
