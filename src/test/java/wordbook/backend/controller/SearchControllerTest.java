package wordbook.backend.controller;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import wordbook.backend.api.service.ApiService;
import wordbook.backend.api.service.ChatClientService;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.word.service.WordService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)

class SearchControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private WordService wordService;
    @MockitoBean
    private ChatClientService chatClientService;

    @Test
    @WithMockUser(username="username")
    public void searchWord() throws Exception {
        //given
        String lang="en";
        String keyword="test";
        WordResponseDTO wordResponseDTO=new WordResponseDTO();
        wordResponseDTO.setWord("test");
        wordResponseDTO.setLang(lang);
        given(userService.possibleUsage(any())).willReturn(true);
        given(chatClientService.getResponse(anyString(),anyString())).willReturn(wordResponseDTO);
        given(wordService.createWord(wordResponseDTO,"username")).willReturn(wordResponseDTO);

        //when

        mockMvc.perform(get("/api/v2/openai/search")
                        .param("keyword", keyword)
                        .param("lang", lang))
                .andExpect(status().isOk())
                ;
    }
}