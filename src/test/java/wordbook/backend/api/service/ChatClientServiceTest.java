package wordbook.backend.api.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class ChatClientServiceTest {
    @Mock
    ChatClient.Builder chatClient;
    @Mock
     ObjectMapper objectMapper;
    @InjectMocks
    ChatClientService chatClientService;

    @Test
    public void creatContent(){
        //given
        String word="wordbook";
        String lang="en";
        String meaning="단어장";
        String topic="학습";
        //when
        String text=chatClientService.createContent(word,lang,meaning,topic);
        //then
        System.out.println("text = " + text);
    }

}