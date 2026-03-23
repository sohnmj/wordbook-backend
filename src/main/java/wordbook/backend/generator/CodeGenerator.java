package wordbook.backend.generator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;

@Component
@Slf4j
public class CodeGenerator {
    private static final SecureRandom random = new SecureRandom();
    public String generateCode(){

        //4자리 난수 생성
        int code=random.nextInt(9000)+1000;
        return String.valueOf(code);
    }
}
