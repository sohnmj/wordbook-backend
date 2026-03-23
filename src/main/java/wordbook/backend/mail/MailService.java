package wordbook.backend.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import wordbook.backend.generator.CodeGenerator;
import wordbook.backend.generator.MailGenerator;
import wordbook.backend.redis.RedisService;
@Slf4j
@Service
public class MailService {

    private static final String EMAIL_PREFIX = "email:";
    private static final Long TTl=300l;

    //의존관계
    private final JavaMailSender mailSender;
    private final RedisService redisService;
    private final CodeGenerator codeGenerator;
    private final MailGenerator mailGenerator;

    public MailService(RedisService redisService,JavaMailSender mailSender, CodeGenerator codeGenerator, MailGenerator mailGenerator) {
        this.redisService = redisService;
        this.mailSender = mailSender;
        this.codeGenerator = codeGenerator;
        this.mailGenerator = mailGenerator;
    }

    //이메일 인증을 위한 이메일 전송
    public void sendMail(String to){

        String code=codeGenerator.generateCode();
        //이메일 생성
        SimpleMailMessage message = mailGenerator.generateMailMessage(to,code);

        //이메일 전송
        try{
            log.info("이메일 전송");
            mailSender.send(message);
            //이메일 전송시 유효시간 동안 redis에 해당 이메일 계정 저장
            redisService.setRedis(EMAIL_PREFIX+to,code,TTl);
            log.info("이메일 전송 성공");
        }catch(MailException e){
            log.error("이메일 전송 실패");
            throw new RuntimeException("email failed",e);
        }
    }
    public boolean verifyMail(String to,String code){
        return redisService.verify(EMAIL_PREFIX+to,code);
    }

}
