package wordbook.backend.controller;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import wordbook.backend.api.service.ApiService;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.domain.word.service.WordService;
import wordbook.backend.usage.UsageService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = true)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiService apiService;
    @MockitoBean
    private WordService wordService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private UsageService usageService;

    @Test
    @WithMockUser(username = "testUser")
    void 사용량이_0이면_AI를_호출하지_않고_예외가_발생한다() {
        given(userService.possibleUsage("testUser")).willReturn(false);

        assertThatThrownBy(() -> mockMvc.perform(get("/api/v2/openai/search")
                .param("keyword", "apple")
                .param("lang", "en")))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("일일 이용 횟수를 모두 소진하였습니다.");

        // 비용이 드는 AI 호출, 단어 저장, 차감이 전혀 일어나지 않아야 한다
        verify(apiService, never()).getResponseEX(anyString(), anyString(), any(), any());
        verify(wordService, never()).createWord(any(), anyString());
        verify(usageService, never()).decreaseUsage(anyString());
    }

    @Test
    @WithMockUser(username = "testUser")
    void 사용량이_있으면_AI를_호출하고_차감한다() throws Exception {
        given(userService.possibleUsage("testUser")).willReturn(true);

        mockMvc.perform(get("/api/v2/openai/search")
                .param("keyword", "apple")
                .param("lang", "en"));

        verify(apiService).getResponseEX("apple", "en", null, null);
        verify(usageService).decreaseUsage("testUser");
    }
}
