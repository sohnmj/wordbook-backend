package wordbook.backend.generator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

@Component
public class MailGenerator {
    @Value("${spring.mail.username}")
    private String fromEmail;
    private static final String TITLE = "인증 번호를 입력해주세요";

    public SimpleMailMessage generateMailMessage(String to, String code){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(TITLE);
        message.setText(String.valueOf(code));
        message.setFrom(fromEmail);
        return message;
    }
}
