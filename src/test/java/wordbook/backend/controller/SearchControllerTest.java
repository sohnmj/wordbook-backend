package wordbook.backend.controller;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import wordbook.backend.api.service.ApiService;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.word.dto.WordSearchRequestDTO;
import wordbook.backend.domain.word.service.WordService;
import wordbook.backend.error.api.AIUnavailableException;
import wordbook.backend.error.api.UsageExhaustedException;
import wordbook.backend.usage.UsageService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = true)
class SearchControllerTest {

    private static final String URL = "/api/v2/openai/search";
    private static final String USER = "testUser";
    private static final String VALID_BODY = """
            {"keyword": "apple", "lang": "en"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiService apiService;
    @MockitoBean
    private WordService wordService;
    @MockitoBean
    private UsageService usageService;

    //JSON body로 검색 요청을 보낸다
    private ResultActions 검색_요청(String body) throws Exception {
        return mockMvc.perform(post(URL)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // ===== 사용량·환불 흐름 =====

    @Test
    @WithMockUser(username = USER)
    void 사용량이_소진되면_429를_응답하고_AI호출_저장_환불이_없다() throws Exception {
        willThrow(new UsageExhaustedException()).given(usageService).decreaseUsage(USER);

        검색_요청(VALID_BODY)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("USAGE_EXHAUSTED"));

        verify(apiService, never()).getResponseEX(any());
        verify(wordService, never()).createWord(any(), anyString());
        verify(usageService, never()).refundUsage(anyString());
    }

    @Test
    @WithMockUser(username = USER)
    void AI호출이_실패하면_환불하고_503을_응답한다() throws Exception {
        given(apiService.getResponseEX(any()))
                .willThrow(new AIUnavailableException(new RuntimeException("OpenAI 장애")));

        검색_요청(VALID_BODY)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_UNAVAILABLE"));

        verify(usageService, times(1)).refundUsage(USER);
        verify(wordService, never()).createWord(any(), anyString());
    }

    @Test
    @WithMockUser(username = USER)
    void 단어_저장이_실패하면_환불한다() {
        given(apiService.getResponseEX(any())).willReturn(WordResponseDTO.builder().build());
        given(wordService.createWord(any(), eq(USER))).willThrow(new IllegalStateException("저장 실패"));

        // 저장 실패의 응답 코드(500/503)는 아직 미정이라, 처리되지 않은 예외가 그대로 전파되는 현재 동작만 확인한다
        assertThatThrownBy(() -> 검색_요청(VALID_BODY))
                .hasRootCauseMessage("저장 실패");

        verify(usageService, times(1)).refundUsage(USER);
    }

    @Test
    @WithMockUser(username = USER)
    void 성공하면_200을_응답하고_환불하지_않는다() throws Exception {
        given(apiService.getResponseEX(any())).willReturn(WordResponseDTO.builder().build());
        given(wordService.createWord(any(), eq(USER))).willReturn(WordResponseDTO.builder().word("apple").build());

        검색_요청(VALID_BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.word").value("apple"));

        verify(apiService).getResponseEX(new WordSearchRequestDTO("apple", "en", null, null));
        verify(usageService, never()).refundUsage(anyString());
    }

    @Test
    @WithMockUser(username = USER)
    void 사용량_차감이_AI호출보다_먼저_일어난다() throws Exception {
        given(apiService.getResponseEX(any())).willReturn(WordResponseDTO.builder().build());

        검색_요청(VALID_BODY);

        InOrder order = inOrder(usageService, apiService, wordService);
        order.verify(usageService).decreaseUsage(USER);
        order.verify(apiService).getResponseEX(any());
        order.verify(wordService).createWord(any(), eq(USER));
    }

    // ===== 입력 검증: 실패하면 차감도 일어나지 않아야 한다 =====

    private void 아무것도_호출되지_않았다() {
        verify(usageService, never()).decreaseUsage(anyString());
        verify(apiService, never()).getResponseEX(any());
        verify(wordService, never()).createWord(any(), anyString());
    }

    @Test
    @WithMockUser(username = USER)
    void keyword가_공백이면_400과_필드_사유를_응답한다() throws Exception {
        검색_요청("""
                {"keyword": " ", "lang": "en"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value(containsString("keyword")));

        아무것도_호출되지_않았다();
    }

    @Test
    @WithMockUser(username = USER)
    void keyword가_50자를_넘으면_400을_응답한다() throws Exception {
        검색_요청("""
                {"keyword": "%s", "lang": "en"}
                """.formatted("a".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value(containsString("keyword")));

        아무것도_호출되지_않았다();
    }

    @Test
    @WithMockUser(username = USER)
    void lang이_없으면_400을_응답한다() throws Exception {
        검색_요청("""
                {"keyword": "apple"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value(containsString("lang")));

        아무것도_호출되지_않았다();
    }

    @Test
    @WithMockUser(username = USER)
    void body가_JSON이_아니면_400을_응답한다() throws Exception {
        검색_요청("not-json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        아무것도_호출되지_않았다();
    }
}
