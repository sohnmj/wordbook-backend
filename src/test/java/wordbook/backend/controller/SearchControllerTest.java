package wordbook.backend.controller;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.word.dto.WordSearchRequestDTO;
import wordbook.backend.error.ErrorCode;
import wordbook.backend.error.api.RequestInProgressException;
import wordbook.backend.error.api.TemporarilyUnavailableException;
import wordbook.backend.error.api.UsageExhaustedException;
import wordbook.backend.search.SearchService;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//HTTP 경계만 검증한다: 헤더·body 검증, 서비스 호출 인자, 예외 → 상태 코드 매핑. 검색 흐름 자체는 SearchServiceTest에서 본다
@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = true)
class SearchControllerTest {

    private static final String URL = "/api/v2/openai/search";
    private static final String USER = "testUser";
    private static final String IDEM_KEY = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
    private static final String VALID_BODY = """
            {"keyword": "apple", "lang": "en"}
            """;
    private static final WordSearchRequestDTO VALID_REQUEST = new WordSearchRequestDTO("apple", "en", null, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    private MockHttpServletRequestBuilder 요청(String body) {
        return post(URL)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    //멱등성 키 헤더를 붙여 검색 요청을 보낸다
    private ResultActions 검색_요청(String body) throws Exception {
        return mockMvc.perform(요청(body).header("Idempotency-Key", IDEM_KEY));
    }

    private void 서비스가_호출되지_않았다() {
        verify(searchService, never()).search(anyString(), anyString(), any());
    }

    // ===== 정상 =====

    @Test
    @WithMockUser(username = USER)
    void 올바른_요청이면_200과_검색_결과를_응답하고_사용자명_키_body를_넘긴다() throws Exception {
        given(searchService.search(USER, IDEM_KEY, VALID_REQUEST))
                .willReturn(WordResponseDTO.builder().word("apple").build());

        검색_요청(VALID_BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.word").value("apple"));

        verify(searchService).search(USER, IDEM_KEY, VALID_REQUEST);
    }

    @Test
    @WithMockUser(username = USER)
    void 대문자_UUID는_소문자로_바꿔서_넘긴다() throws Exception {
        mockMvc.perform(요청(VALID_BODY).header("Idempotency-Key", IDEM_KEY.toUpperCase()))
                .andExpect(status().isOk());

        verify(searchService).search(USER, IDEM_KEY, VALID_REQUEST);
    }

    // ===== 멱등성 키 헤더 검증 =====

    @Test
    @WithMockUser(username = USER)
    void 멱등성_키_헤더가_없으면_400과_헤더_이름을_응답한다() throws Exception {
        mockMvc.perform(요청(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value(containsString("Idempotency-Key")));

        서비스가_호출되지_않았다();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",                                          // 빈 값
            "abc",                                       // UUID 아님
            "3f2b8c1e9a4d4e6f8b2a1c3d5e7f9a0b",          // 하이픈 없음
            "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b0",     // 한 글자 더 김
            "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0g",      // hex 아닌 문자
            "3f2b8c1e-9a4d-4e6f-8b2a:1c3d5e7f9a0b",      // Redis 키 구분자 ':' 포함
    })
    @WithMockUser(username = USER)
    void 멱등성_키가_UUID_형식이_아니면_400을_응답한다(String invalidKey) throws Exception {
        mockMvc.perform(요청(VALID_BODY).header("Idempotency-Key", invalidKey))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        서비스가_호출되지_않았다();
    }

    // ===== 서비스 예외 → 상태 코드 매핑 =====

    static Stream<Arguments> 서비스_예외와_응답() {
        return Stream.of(
                Arguments.of(new RequestInProgressException(), ErrorCode.REQUEST_IN_PROGRESS),
                Arguments.of(new UsageExhaustedException(), ErrorCode.USAGE_EXHAUSTED),
                Arguments.of(new TemporarilyUnavailableException(new RuntimeException("Redis 장애")), ErrorCode.TEMPORARILY_UNAVAILABLE)
        );
    }

    @ParameterizedTest
    @MethodSource("서비스_예외와_응답")
    @WithMockUser(username = USER)
    void 서비스_예외는_ErrorCode의_상태와_코드로_응답한다(RuntimeException ex, ErrorCode errorCode) throws Exception {
        given(searchService.search(USER, IDEM_KEY, VALID_REQUEST)).willThrow(ex);

        검색_요청(VALID_BODY)
                .andExpect(status().is(errorCode.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(errorCode.name()));
    }

    // ===== body 검증: 실패하면 서비스가 호출되지 않아야 한다 =====

    @Test
    @WithMockUser(username = USER)
    void keyword가_공백이면_400과_필드_사유를_응답한다() throws Exception {
        검색_요청("""
                {"keyword": " ", "lang": "en"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value(containsString("keyword")));

        서비스가_호출되지_않았다();
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

        서비스가_호출되지_않았다();
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

        서비스가_호출되지_않았다();
    }

    @Test
    @WithMockUser(username = USER)
    void body가_JSON이_아니면_400을_응답한다() throws Exception {
        검색_요청("not-json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        서비스가_호출되지_않았다();
    }
}
