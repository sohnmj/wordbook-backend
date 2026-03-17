//package wordbook.backend.controller;
//
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
//import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//import tools.jackson.databind.ObjectMapper;
//import wordbook.backend.domain.wordbookword.dto.WordBookWordRequestDTO;
//import wordbook.backend.domain.wordbookword.service.WordBookWordService;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.BDDMockito.given;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(WordBookWordController.class)
//@AutoConfigureMockMvc(addFilters = true)
//class WordBookWordControllerTest {
//    @MockitoBean
//    private WordBookWordService wordBookWordService;
//    @Autowired
//    private MockMvc mockMvc;
//    @Autowired
//    private ObjectMapper objectMapper;
//    @Test
//    @WithMockUser(username = "admin1", roles = "USER")
//    public void getAllWordBookWord() throws Exception {
//        // given
//        WordBookWordRequestDTO requestDTO = new WordBookWordRequestDTO();
//        requestDTO.setWord(1L);
//        requestDTO.setWordbook(10L);
//
//        given(wordBookWordService.createWordBookWord(
//                anyLong(),
//                anyLong(),
//                null
//        )).willReturn(100L);
//        // when & then
//        mockMvc.perform(post("/wordbookword")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDTO)))
//                .andExpect(status().isOk())
//                .andExpect(content().string("100")).andDo(print());
//
//    }
//
//}