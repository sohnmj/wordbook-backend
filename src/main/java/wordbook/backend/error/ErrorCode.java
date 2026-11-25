package wordbook.backend.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
@AllArgsConstructor
@Getter
public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    USAGE_EXHAUSTED(HttpStatus.TOO_MANY_REQUESTS, "일일 이용 횟수를 모두 소진하였습니다."),
    REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "이전 요청을 처리 중입니다. 잠시 후 다시 시도해 주세요."),
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,"예문 생성에 실패했습니다. 잠시 후 다시 시도해 주세요."),
    TEMPORARILY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "일시적으로 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해 주세요.");
    private final HttpStatus httpStatus;
    private final String message;
}
