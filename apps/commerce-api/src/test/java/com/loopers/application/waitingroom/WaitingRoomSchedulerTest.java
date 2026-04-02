package com.loopers.application.waitingroom;

import static org.mockito.Mockito.verify;

import com.loopers.domain.waitingroom.WaitingRoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("WaitingRoomScheduler 단위 테스트")
@ExtendWith(MockitoExtension.class)
class WaitingRoomSchedulerTest {

    @Mock
    WaitingRoomService waitingRoomService;

    @InjectMocks
    WaitingRoomScheduler waitingRoomScheduler;

    @DisplayName("스케줄러 실행 시 WaitingRoomService.processQueue()를 호출한다.")
    @Test
    void run_callsProcessQueue() {
        // act
        waitingRoomScheduler.run();

        // assert
        verify(waitingRoomService).processQueue();
    }
}
