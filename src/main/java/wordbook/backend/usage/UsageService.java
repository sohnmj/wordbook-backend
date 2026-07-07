package wordbook.backend.usage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.redis.RedisService;

@Service
public class UsageService {
    private final RedisService redisService;
    private final UserService userService;
    private final String USAGE="usage:";
    public UsageService(RedisService redisService, UserService userService) {
        this.redisService = redisService;
        this.userService = userService;
    }
    @Transactional
    public boolean getUsage(String username) {
        UserEntity user = userService.findUserByUsername(username);
        long usage = redisService.getUsage(USAGE + username);
        if(usage==1){
            userService.updateUsage(user);
            return true;
        }
        else {
            return false;
        }
    }
    @Transactional(readOnly = true)
    public int HowManyUsage(String username) {
        UserEntity user = userService.findUserByUsername(username);
        return user.getUsage();
    }

    @Transactional
    public void decreaseUsage(String username){
        UserEntity user = userService.findUserByUsername(username);
        userService.decreaseUsage(user);
    }
}
