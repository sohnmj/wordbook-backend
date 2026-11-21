package wordbook.backend.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.json.JsonMapper;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.word.dto.WordSearchRequestDTO;
import wordbook.backend.error.ErrorCode;
import wordbook.backend.error.api.AIUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

//OpenAI 호출·응답 파싱 실패가 AIUnavailableException으로 변환되는지 확인 (실제 OpenAI 호출 없음)
class ChatClientServiceExceptionTest {

    private ChatClient chatClient;
    private ChatClientService chatClientService;
    private final WordSearchRequestDTO request = new WordSearchRequestDTO("apple", "en", "사과", "과일");

    @BeforeEach
    void setUp() {
        // prompt().system().user().call().content() 체인을 한 번에 stub하기 위해 deep stubs 사용
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        given(builder.build()).willReturn(chatClient);
        chatClientService = new ChatClientService(builder, JsonMapper.builder().build());
    }

    private void AI_응답(String content) {
        given(chatClient.prompt().system(anyString()).user(anyString()).call().content()).willReturn(content);
    }

    @Test
    void 정상_JSON이면_DTO로_변환한다() {
        AI_응답("""
                {"word":"apple","useword":"apple","meaning":"사과","topic":"과일",
                 "example":"I ate an apple.","translation":"나는 사과를 먹었다."}
                """);

        WordResponseDTO result = chatClientService.getResponseEX(request);

        assertThat(result.getExample()).isEqualTo("I ate an apple.");
        assertThat(result.getUseword()).isEqualTo("apple");
        assertThat(result.getLang()).isEqualTo("en");
    }

    @Test
    void OpenAI_호출이_실패하면_원인을_담아_AIUnavailableException을_던진다() {
        RuntimeException original = new RuntimeException("HTTP 500 - 서버 오류");
        given(chatClient.prompt().system(anyString()).user(anyString()).call().content()).willThrow(original);

        assertThatThrownBy(() -> chatClientService.getResponseEX(request))
                .isInstanceOf(AIUnavailableException.class)
                .hasCause(original)
                .extracting("errorCode").isEqualTo(ErrorCode.AI_UNAVAILABLE);
    }

    @Test
    void 응답이_JSON이_아니면_AIUnavailableException을_던진다() {
        AI_응답("죄송합니다, 예문을 만들 수 없습니다.");

        assertThatThrownBy(() -> chatClientService.getResponseEX(request))
                .isInstanceOf(AIUnavailableException.class)
                .cause().isNotNull();
    }
}
