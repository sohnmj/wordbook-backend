package wordbook.backend.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final String VERIFY="verify";
    private final String API_USAGE="API:usage";
    private final String USERNAME="username";
    private final String PASSWORD="password";
    private final StringRedisTemplate stringRedisTemplate;

    public RedisService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;

    }
    //redis 저장
    public void setRedis(String key, String value,Long TTL) {
        stringRedisTemplate.opsForValue().set(key,value,TTL, TimeUnit.SECONDS);
    }

    //이메일 코드 인증
    public boolean verify(String email,String code){
        //email에 해당하는 인증코드 가져오기
        String savedCode = stringRedisTemplate.opsForValue().get(email);

        //redis에 해당 이메일이 없으면 false
        if(savedCode == null){
            return false;
        }

        //해당 이메일 인증코드와 code가 같다면 true
        if(savedCode.equals(code)){
            //인증된 이메일 정보는 300초까지 인증이 적용되고 그전에 회원가입을 끝내야함
            setRedis(email,VERIFY,300L);
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
    //usage 소진
    public Long decreaseCupon(){
        if(stringRedisTemplate.opsForValue().get(API_USAGE)==null){
            stringRedisTemplate.opsForValue().set(API_USAGE,"100");
        }
        return stringRedisTemplate.opsForValue().decrement(API_USAGE);
    }

    public boolean isDuplicate(String username){
        if(stringRedisTemplate.opsForValue().get("usage:"+username)==null){
            return false;
        }
        return true;
    }
}
