package wordbook.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import wordbook.backend.domain.user.dto.UserCreateDTO;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.repository.UserRepository;
import wordbook.backend.domain.user.service.UserService;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;

class UserServiceTest {
    @Test
    void security() throws InterruptedException{
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        var testingToken=new TestingAuthenticationToken("sohnmj","12345","ROLE_USER");
        var securityContext=SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(testingToken);
        SecurityContextHolder.setContext(securityContext);

        var contextArray=new SecurityContext[1];
        var thread= new Thread(()->{
            var context=SecurityContextHolder.getContext();
            contextArray[0]=context;
        });
thread.start();
        thread.join();
        var result=SecurityContextHolder.getContext();
        assertEquals(securityContext,contextArray[0]);
        assertEquals(testingToken,contextArray[0].getAuthentication());

    }

}