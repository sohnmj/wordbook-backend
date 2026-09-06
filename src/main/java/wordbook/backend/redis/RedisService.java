package wordbook.backend.redis;

import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final String VERIFIED_EMAIL="verify";
    private final String API_USAGE="wordbook:ai_api:limit";
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> GET_API_SCRIPT;

    public RedisService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.GET_API_SCRIPT= new DefaultRedisScript<>();
        this.GET_API_SCRIPT.setScriptText(
                "if redis.call('EXISTS', KEYS[2]) == 0 then redis.call('SET', KEYS[2], ARGV[2]) end " +
                        "if redis.call('EXISTS', KEYS[1]) == 1 then return -1 end " +
                        "local current_count = redis.call('DECR', KEYS[2]) " +
                        "if current_count >= 0 then " +
                        "redis.call('SET', KEYS[1], 'true', 'EX', ARGV[1]) " +
                        "return 1 " +
                        "else " +
                        "return -1 " +
                        "end"
        );
        this.GET_API_SCRIPT.setResultType(Long.class);
    }
    //redis 저장
    public void setRedis(String key, String value,Long TTL) {
        stringRedisTemplate.opsForValue().set(key,value,TTL, TimeUnit.SECONDS);
    }
    //redis 읽기
    public String getRedis(String key){
        return stringRedisTemplate.opsForValue().get(key);
    }

    //이메일 코드 인증
    public boolean verify(String emailKey,String code){
        //email에 해당하는 인증코드 가져오기
        String savedCode = stringRedisTemplate.opsForValue().get(emailKey);

        //redis에 해당 이메일이 없으면 false
        if(savedCode == null){
            return false;
        }

        //해당 이메일 인증코드와 code가 같다면 true
        if(savedCode.equals(code)){
            //인증된 이메일 정보는 300초까지 인증이 적용되고 그전에 회원가입을 끝내야함
            setRedis(emailKey,VERIFIED_EMAIL,300L);
            return true;
        }
        //인증코드와 code가 다르다면 false
        return false;
    }
    public boolean IsVerifiedEmail(String email){
        String verify = stringRedisTemplate.opsForValue().get(email);
        if(verify == null){
            return false;
        }
        boolean equals = "verify".equals(verify);
        if(equals){
            stringRedisTemplate.delete(email);
        }
        return equals;
    }

    public boolean isTrue(String key){
        if(stringRedisTemplate.opsForValue().get(key)==null){
            return false;
        }
        return true;
    }
    
    
    //rua script 실행
    @Transactional
    public long getUsage(String usageKey) {


        // 2. 루아 스크립트 실행 (원자적 연산)
        Long result = stringRedisTemplate.execute(
                GET_API_SCRIPT,
                Arrays.asList(usageKey, API_USAGE),
                "86400",
                "100"
        );

        return result;
    }
}
