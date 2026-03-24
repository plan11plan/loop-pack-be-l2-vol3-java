package com.loopers.application.user;

import com.loopers.application.user.dto.UserCriteria;
import com.loopers.application.user.dto.UserResult;
import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.SignupCouponPolicy;
import com.loopers.domain.notification.NotificationSender;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;
    private final CouponService couponService;
    private final NotificationSender notificationSender;

    @Transactional
    public UserResult signup(UserCriteria.Signup criteria) {
        UserModel userModel = userService.signup(
            criteria.loginId(), criteria.rawPassword(), criteria.name(), criteria.birthDate(), criteria.email()
        );
        userService.addPoint(userModel.getId(), 1000L);
        CouponModel welcomeCoupon = couponService.register(SignupCouponPolicy.WELCOME.toCreateCommand());
        couponService.issue(welcomeCoupon.getId(), userModel.getId());
        notificationSender.send(
                userModel.getEmail(),
                "회원가입을 축하합니다!",
                userModel.getName() + "님, 회원가입을 축하합니다!");
        return UserResult.from(userModel);
    }

    @Transactional(readOnly = true)
    public UserResult getMyInfo(String loginId) {
        UserModel user = userService.getByLoginId(loginId);
        return UserResult.from(user);
    }

    @Transactional
    public void changePassword(String loginId, UserCriteria.ChangePassword criteria) {
        userService.changePassword(loginId, criteria.rawCurrentPassword(), criteria.rawNewPassword());
    }
}
