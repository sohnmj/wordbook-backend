//package wordbook.backend;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.MockitoAnnotations;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.ai.chat.client.ChatClient;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
//import tools.jackson.databind.ObjectMapper;
//import wordbook.backend.api.ApiResponseDTO;
//import wordbook.backend.api.service.ChatClientService;
//import wordbook.backend.domain.word.dto.WordResponseDTO;
//
//
//@SpringBootTest
//class ChatClientServiceTest {
//
//    @Autowired
//    private ChatClientService chatClientService;
//    @Test
//    void testGetResponse_withStubbedCallApi() throws Exception {
//        //given
//        String word="침투하다";
//        String lang="en";
//        //then
//        WordResponseDTO response = chatClientService.getResponse(word, lang);
//        System.out.println("response.getWord() = " + response.getWord());
//        System.out.println("response.getLang() = " + response.getLang());
//        System.out.println("response.getAntonym() = " + response.getAntonym());
//        System.out.println("response.getExample() = " + response.getExample());
//        System.out.println("response.getSynonym() = " + response.getSynonym());
//        System.out.println("response.getMeaning() = " + response.getMeaning());
//    }
//}