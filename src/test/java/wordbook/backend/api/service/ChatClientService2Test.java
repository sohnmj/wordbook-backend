package wordbook.backend.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import wordbook.backend.domain.word.dto.WordResponseDTO;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class ChatClientService2Test {
    @Autowired
    ChatClientService  chatClientService;
    @Test
    public void callapi(){
        //given
        String word="wordbook";
        String lang="en";
        String meaning="단어장";
        String topic="학습";
        //when
        WordResponseDTO responseEX = chatClientService.getResponseEX(word, lang, meaning, topic);

        System.out.println("responseEX.getWord() = " + responseEX.getWord());
        System.out.println("responseEX.getExample() = " + responseEX.getExample());
        System.out.println("responseEX.getTranslation() = " + responseEX.getTranslation());
    }
    
}