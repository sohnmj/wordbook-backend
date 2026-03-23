package wordbook.backend.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UsageScheduler {
    private final StringRedisTemplate stringRedisTemplate;
    public UsageScheduler(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Scheduled(cron="0 0 10 * * *")
    public void addUsage() {
        stringRedisTemplate.opsForValue().set("API:usage", "100");
    }
}
