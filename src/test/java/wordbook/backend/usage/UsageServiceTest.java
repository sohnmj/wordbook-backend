package wordbook.backend.usage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.redis.RedisService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {
    @Mock
    private RedisService redisService;
    @Mock
    private UserService userService;
    @InjectMocks
    private UsageService usageService;
    @Test
    void getUsage(){
        //given
        String username = "username";
        when(userService.findUserByUsername(any())).thenReturn(new UserEntity());
        when(redisService.isDuplicate(any())).thenReturn(false);
        when(redisService.decreaseCupon()).thenReturn(1l);
        //when
        boolean usage = usageService.getUsage(username);
        //then
        assertTrue(usage);
    }
    @Test
    void getUsage2(){
        String username = "username";
        when(userService.findUserByUsername(any())).thenReturn(new UserEntity());
        when(redisService.isDuplicate(any())).thenReturn(false);
        when(redisService.decreaseCupon()).thenReturn(-1l);
        //when
        boolean usage = usageService.getUsage(username);
        //then
        assertFalse(usage);
    }
}