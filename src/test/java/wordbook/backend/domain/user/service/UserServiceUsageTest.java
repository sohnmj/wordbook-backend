package wordbook.backend.domain.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import wordbook.backend.domain.user.repository.UserRepository;
import wordbook.backend.error.ErrorCode;
import wordbook.backend.error.api.UsageExhaustedException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceUsageTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Test
    void 잔량이_없으면_차감에_실패하고_USAGE_EXHAUSTED를_던진다() {
        // 조건부 UPDATE(usage > 0)가 영향 행 0을 반환하는 상황
        given(userRepository.consumeUsage("testUser")).willReturn(0);

        assertThatThrownBy(() -> userService.consumeUsage("testUser"))
                .isInstanceOf(UsageExhaustedException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USAGE_EXHAUSTED);
    }

    @Test
    void 잔량이_있으면_차감에_성공한다() {
        given(userRepository.consumeUsage("testUser")).willReturn(1);

        assertThatCode(() -> userService.consumeUsage("testUser")).doesNotThrowAnyException();
    }

    @Test
    void 환불하면_환불_쿼리를_호출한다() {
        given(userRepository.refundUsage("testUser")).willReturn(1);

        userService.refundUsage("testUser");

        verify(userRepository).refundUsage("testUser");
    }
}
