package wordbook.backend.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import wordbook.backend.generator.CodeGenerator;
import wordbook.backend.generator.MailGenerator;
import wordbook.backend.redis.RedisService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class MailServiceTest {
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private CodeGenerator codeGenerator;
    @Mock
    private MailGenerator  mailGenerator;
    @Mock
    private RedisService redisService;
    @InjectMocks
    private MailService mailService;
    @Test
    void sendMail()
    {
// given
        String to = "test@email.com";
        String code = "1234";
        SimpleMailMessage message = new SimpleMailMessage();

        when(codeGenerator.generateCode()).thenReturn(code);
        when(mailGenerator.generateMailMessage(to, code)).thenReturn(message);

        // when
        mailService.sendMail(to);

        // then ⭐ 핵심 검증
        verify(mailSender).send(message);
        verify(redisService).setRedis("email:" + to, code, 300L);
    }
}