package com.loopers.interfaces.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

@DisplayName("AuthUserArgumentResolver 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AuthUserArgumentResolverTest {

    private AuthUserArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AuthUserArgumentResolver();
    }

    @DisplayName("supportsParameter 메서드는")
    @Nested
    class SupportsParameter {

        @DisplayName("@Auth 어노테이션과 AuthUser 타입이면 true를 반환한다")
        @Test
        void returnsTrue_whenAuthAnnotationAndAuthUserType() throws Exception {
            // arrange
            MethodParameter parameter = new MethodParameter(
                TestController.class.getMethod("testMethod", AuthUser.class), 0
            );

            // act & assert
            assertThat(resolver.supportsParameter(parameter)).isTrue();
        }

        @DisplayName("@Auth 어노테이션이 없으면 false를 반환한다")
        @Test
        void returnsFalse_whenNoAuthAnnotation() throws Exception {
            // arrange
            MethodParameter parameter = new MethodParameter(
                TestController.class.getMethod("noAnnotationMethod", AuthUser.class), 0
            );

            // act & assert
            assertThat(resolver.supportsParameter(parameter)).isFalse();
        }
    }

    @DisplayName("resolveArgument 메서드는")
    @Nested
    class ResolveArgument {

        @DisplayName("request attribute에 AuthUser가 있으면 반환한다")
        @Test
        void returnsAuthUser_whenAttributeExists() throws Exception {
            // arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            AuthUser expectedAuthUser = new AuthUser(1L, "testuser1", "홍길동");
            request.setAttribute("authUser", expectedAuthUser);

            NativeWebRequest webRequest = new ServletWebRequest(request);
            MethodParameter parameter = new MethodParameter(
                TestController.class.getMethod("testMethod", AuthUser.class), 0
            );

            // act
            Object result = resolver.resolveArgument(parameter, null, webRequest, null);

            // assert
            assertThat(result).isInstanceOf(AuthUser.class);
            AuthUser authUser = (AuthUser) result;
            assertThat(authUser.id()).isEqualTo(1L);
            assertThat(authUser.loginId()).isEqualTo("testuser1");
            assertThat(authUser.name()).isEqualTo("홍길동");
        }

        @DisplayName("request attribute에 AuthUser가 없으면 CoreException(UNAUTHORIZED)을 던진다")
        @Test
        void throwsUnauthorized_whenAttributeNotExists() throws Exception {
            // arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            NativeWebRequest webRequest = new ServletWebRequest(request);
            MethodParameter parameter = new MethodParameter(
                TestController.class.getMethod("testMethod", AuthUser.class), 0
            );

            // act & assert
            assertThatThrownBy(() -> resolver.resolveArgument(parameter, null, webRequest, null))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorType.UNAUTHORIZED);
        }
    }

    // 테스트용 컨트롤러
    static class TestController {
        public void testMethod(@Auth AuthUser authUser) {}
        public void noAnnotationMethod(AuthUser authUser) {}
    }
}
