package wordbook.backend.domain.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceUsageTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Test
    void 사용량이_0이면_차감에_실패한다() {
        // 조건부 UPDATE(usage > 0)가 영향 행 0을 반환하는 상황
        given(userRepository.consumeUsage("testUser")).willReturn(0);

        assertThatThrownBy(() -> userService.consumeUsage("testUser"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("일일 이용 횟수를 모두 소진하였습니다.");
    }

    @Test
    void 사용량이_있으면_차감에_성공한다() {
        given(userRepository.consumeUsage("testUser")).willReturn(1);

        assertThatCode(() -> userService.consumeUsage("testUser")).doesNotThrowAnyException();
    }

    @Test
    void 사용량이_0이면_사용_불가능하다() {
        UserEntity user = UserEntity.builder().username("testUser").usage(0).build();
        given(userRepository.findByUsername("testUser")).willReturn(Optional.of(user));

        assertThat(userService.possibleUsage("testUser")).isFalse();
    }

    @Test
    void 일일_지급을_받으면_사용량이_늘어난다() {
        UserEntity user = UserEntity.builder().username("testUser").usage(0).build();
        given(userRepository.findByUsername("testUser")).willReturn(Optional.of(user));

        userService.grantDailyUsage("testUser");

        assertThat(user.getUsage()).isEqualTo(UserEntity.DAILY_GRANT);
        assertThat(userService.possibleUsage("testUser")).isTrue();
    }
}
