package wordbook.backend.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

//멱등성 키의 값 형식(IN_PROGRESS / DONE:<json>)과 분기를 검증한다. 실제 Redis 명령 동작은 확인하지 않는다
@ExtendWith(MockitoExtension.class)
class RedisServiceIdempotencyTest {

    private static final String KEY = "idem:testUser:3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
    private static final String JSON = "{\"word\":\"apple\"}";

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisService redisService;

    @BeforeEach
    void setUp() {
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        redisService = new RedisService(stringRedisTemplate);
    }

    private void 키_등록_결과(Boolean result) {
        given(valueOperations.setIfAbsent(KEY, "IN_PROGRESS", Duration.ofSeconds(30))).willReturn(result);
    }

    @Test
    void 키를_새로_잡으면_STARTED를_돌려주고_IN_PROGRESS를_TTL_30초로_저장한다() {
        키_등록_결과(true);

        IdempotencyResult result = redisService.tryStart(KEY);

        assertThat(result.status()).isEqualTo(IdempotencyStatus.STARTED);
        verify(valueOperations).setIfAbsent(KEY, "IN_PROGRESS", Duration.ofSeconds(30));
    }

    @Test
    void 이미_DONE이면_접두사를_뺀_JSON과_함께_DONE을_돌려준다() {
        키_등록_결과(false);
        given(valueOperations.get(KEY)).willReturn("DONE:" + JSON);

        IdempotencyResult result = redisService.tryStart(KEY);

        assertThat(result.status()).isEqualTo(IdempotencyStatus.DONE);
        assertThat(result.responseJson()).isEqualTo(JSON);
    }

    @Test
    void 이미_IN_PROGRESS면_IN_PROGRESS를_돌려준다() {
        키_등록_결과(false);
        given(valueOperations.get(KEY)).willReturn("IN_PROGRESS");

        IdempotencyResult result = redisService.tryStart(KEY);

        assertThat(result.status()).isEqualTo(IdempotencyStatus.IN_PROGRESS);
    }

    @Test
    void 키_등록_결과가_null이어도_NPE없이_IN_PROGRESS를_돌려준다() {
        키_등록_결과(null);

        IdempotencyResult result = redisService.tryStart(KEY);

        assertThat(result.status()).isEqualTo(IdempotencyStatus.IN_PROGRESS);
    }

    @Test
    void 등록_실패_후_그_사이_키가_만료돼_값이_없으면_IN_PROGRESS를_돌려준다() {
        // 드문 경우: 감수하기로 한 동작(409)을 고정한다
        키_등록_결과(false);
        given(valueOperations.get(KEY)).willReturn(null);

        IdempotencyResult result = redisService.tryStart(KEY);

        assertThat(result.status()).isEqualTo(IdempotencyStatus.IN_PROGRESS);
    }

    @Test
    void DONE_기록은_접두사를_붙여_TTL_24시간으로_저장한다() {
        redisService.markIdempotencyDone(KEY, JSON);

        verify(valueOperations).set(KEY, "DONE:" + JSON, Duration.ofHours(24));
    }
}
