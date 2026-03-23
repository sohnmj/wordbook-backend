package wordbook.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wordbook.backend.mail.MailRequestDTO;
import wordbook.backend.mail.MailService;
import wordbook.backend.mail.MailVerifyRequestDTO;
import wordbook.backend.redis.RedisService;

@Slf4j
@RestController
@RequestMapping("/api/v2/mail")
public class MailController {
    private MailService mailService;

    @Autowired
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }
    @PostMapping("/")
    public ResponseEntity<Boolean> sendMail(@RequestBody MailRequestDTO dto){
        log.info("email : {}",dto.getEmail());
        mailService.sendMail(dto.getEmail());
        return ResponseEntity.ok(true);
    }
    @PostMapping("/verify")
    public ResponseEntity<Boolean> verifyMail(@RequestBody MailVerifyRequestDTO dto){
        log.info("email : {}",dto.getEmail());
        boolean verified = mailService.verifyMail(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok(verified);
    }


}
