package com.loopers.application.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.application.user.dto.UserCriteria;
import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.dto.CouponCommand;
import com.loopers.domain.notification.NotificationSender;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserFacadeTest {

    @Mock UserService userService;
    @Mock CouponService couponService;
    @Mock NotificationSender notificationSender;
    UserFacade userFacade;

    UserModel stubUser;
    UserCriteria.Signup criteria;

    @BeforeEach
    void setUp() {
        userFacade = new UserFacade(userService, couponService, notificationSender);

        criteria = new UserCriteria.Signup(
                "testuser1", "Test1234!@#", "홍길동", "19900115", "test@example.com");

        stubUser = mock(UserModel.class);
        when(stubUser.getId()).thenReturn(1L);
        when(stubUser.getLoginId()).thenReturn("testuser1");
        when(stubUser.getName()).thenReturn("홍길동");
        when(stubUser.getBirthDate()).thenReturn(LocalDate.of(1990, 1, 15));
        when(stubUser.getEmail()).thenReturn("test@example.com");
        when(stubUser.getPoint()).thenReturn(0L);

        when(userService.signup("testuser1", "Test1234!@#", "홍길동", "19900115", "test@example.com"))
                .thenReturn(stubUser);

        CouponModel stubCoupon = mock(CouponModel.class);
        when(stubCoupon.getId()).thenReturn(100L);
        when(couponService.register(any(CouponCommand.Create.class))).thenReturn(stubCoupon);
    }

    @DisplayName("회원가입할 때, ")
    @Nested
    class Signup {

        @DisplayName("성공하면 웰컴 포인트 1000원이 지급된다.")
        @Test
        void signup_welcomePointAdded() {
            // act
            userFacade.signup(criteria);

            // assert
            verify(userService).addPoint(1L, 1000L);
        }

        @DisplayName("성공하면 웰컴 쿠폰이 생성되고 발급된다.")
        @Test
        void signup_welcomeCouponCreatedAndIssued() {
            // act
            userFacade.signup(criteria);

            // assert
            verify(couponService).register(any(CouponCommand.Create.class));
            verify(couponService).issue(100L, 1L);
        }

        @DisplayName("성공하면 가입 축하 이메일이 발송된다.")
        @Test
        void signup_welcomeEmailSent() {
            // act
            userFacade.signup(criteria);

            // assert
            verify(notificationSender).send(
                    "test@example.com",
                    "회원가입을 축하합니다!",
                    "홍길동님, 회원가입을 축하합니다!");
        }
    }
}
