package wordbook.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import wordbook.backend.domain.wordbooklike.service.WordBookLikeService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WordbookLikeController.class)
@AutoConfigureMockMvc(addFilters = true)
class WordbookLikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WordBookLikeService wordBookLikeService;

    @Test
    @WithMockUser(username = "testUser")
    void 좋아요_추가() throws Exception {

        given(wordBookLikeService.alterWordBookLike(1L, "testUser"))
                .willReturn(true);

        mockMvc.perform(post("/api/v2/wordbooklike/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @WithMockUser(username = "testUser")
    void 좋아요_취소() throws Exception {

        given(wordBookLikeService.alterWordBookLike(1L, "testUser"))
                .willReturn(false);

        mockMvc.perform(post("/api/v2/wordbooklike/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}