package com.loopers.application.user;

import com.loopers.application.user.dto.UserCriteria;
import com.loopers.application.user.dto.UserResult;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import com.loopers.domain.user.event.UserSignedUpEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserResult signup(UserCriteria.Signup criteria) {
        UserModel userModel = userService.signup(
                criteria.loginId(), criteria.rawPassword(), criteria.name(),
                criteria.birthDate(), criteria.email());
        eventPublisher.publishEvent(UserSignedUpEvent.from(userModel));
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
