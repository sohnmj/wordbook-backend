package wordbook.backend.domain.user.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserEntityTest {

    private UserEntity userWithUsage(int usage) {
        return UserEntity.builder().username("testUser").usage(usage).build();
    }

    @Test
    void 사용량이_0이면_차감시_예외가_발생하고_값은_유지된다() {
        UserEntity user = userWithUsage(0);

        assertThatThrownBy(user::consumeUsage)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("일일 이용 횟수를 모두 소진하였습니다.");
        assertThat(user.getUsage()).isZero();
    }

    @Test
    void 사용량이_있으면_1_차감된다() {
        UserEntity user = userWithUsage(3);

        user.consumeUsage();

        assertThat(user.getUsage()).isEqualTo(2);
    }

    @Test
    void 마지막_1회를_쓰면_0이_되고_그_다음은_실패한다() {
        UserEntity user = userWithUsage(1);

        user.consumeUsage();

        assertThat(user.getUsage()).isZero();
        assertThatThrownBy(user::consumeUsage).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void canUse_경계값() {
        assertThat(userWithUsage(0).canUse()).isFalse();
        assertThat(userWithUsage(1).canUse()).isTrue();
    }

    @Test
    void 일일_지급은_10회를_더한다() {
        UserEntity user = userWithUsage(0);

        user.updateDailyUsage();

        assertThat(user.getUsage()).isEqualTo(UserEntity.DAILY_GRANT);
    }
}
