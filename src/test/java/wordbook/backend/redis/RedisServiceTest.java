package wordbook.backend.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class RedisServiceTest {
    @Autowired
    RedisService redisService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Test
    void falseverify(){
        //given
        String code = "code";
        String email = "email";
        assertFalse(redisService.verify(code,email));

    }
    @Test
    void verify(){
        String code = "code";
        String email = "email";
        stringRedisTemplate.opsForValue().set(email,code);
        assertTrue(redisService.verify(email,code));
        stringRedisTemplate.opsForValue().set(email,code+"1");
        assertFalse(redisService.verify(email,code));
    }
    @Test
    void verifyemail(){
        String email = "email1";
        stringRedisTemplate.opsForValue().set(email,"verify");
        assertTrue(redisService.IsVerifiedEmail(email));
    }
    @Test
    void verifyemail1(){
        String email = "email2";
        assertFalse(redisService.IsVerifiedEmail(email));
    }
    @Test
    void decreasecupon(){
        stringRedisTemplate.delete("API:usage");
        assertThat(stringRedisTemplate.opsForValue().get("API:usage")).isNull();
        redisService.decreaseCupon();
        assertThat(stringRedisTemplate.opsForValue().get("API:usage")).isEqualTo("99");

    }
    @Test
    void IsDuplicate(){
        String username = "username";
        stringRedisTemplate.opsForValue().set("usage:"+username,"true");
        boolean duplicate = redisService.isDuplicate(username);
        assertTrue(duplicate);
        stringRedisTemplate.delete("usage:"+username);
        assertFalse(redisService.isDuplicate(username));
    }
}