package wordbook.backend.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest

class ChatClientServiceTest {
    @Autowired
    ChatClientService chatClientService;
    @Test
    public void koreanTest(){
        //given
        String keyword="안녕하세요";
        Boolean korean = chatClientService.isKorean(keyword);
        String keyword1="adfa";
        Boolean korean1 = chatClientService.isKorean(keyword1);
        assertTrue(korean);
        assertFalse(korean1);

    }
    @Test
    public void createContentTest(){
        String keyword="apple";
        String lang="en";
        String content = chatClientService.createContent(keyword, lang, false);
        System.out.println("content = " + content);
    }
}