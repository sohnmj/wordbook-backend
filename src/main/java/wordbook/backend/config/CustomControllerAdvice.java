package wordbook.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import wordbook.backend.error.CustomException;
import wordbook.backend.error.ErrorBody;
import wordbook.backend.error.ErrorCode;

import java.util.stream.Collectors;


@RestControllerAdvice
@Slf4j
public class CustomControllerAdvice {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        log.error("에러 상세:", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("잘못된 요청입니다.");
    }
    //@Valid @RequestBody 검증 실패: 필드별 사유를 응답과 로그에 남긴다 (사용자가 보낸 값은 남기지 않음)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidationException(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("입력값 검증 실패: {}", detail);
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(new ErrorBody(ErrorCode.INVALID_INPUT.name(), detail));
    }
    //body 누락, JSON 문법 오류: 필드 정보가 없으므로 고정 문구 사용
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorBody> handleNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("요청 body를 읽을 수 없음: {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(new ErrorBody(ErrorCode.INVALID_INPUT.name(), ErrorCode.INVALID_INPUT.getMessage()));
    }
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorBody> handleApiException(CustomException customException) {
        ErrorCode errorCode = customException.getErrorCode();

        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("API 에러 code={}", errorCode.name(), customException);   // 스택 트레이스 + cause
        } else {
            log.warn("API 에러 code={}", errorCode.name());                  // 클라이언트 쪽 원인, 한 줄이면 충분
        }

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ErrorBody(errorCode.name(), errorCode.getMessage()));
    }
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorBody> handlerMissingHeaderException(MissingRequestHeaderException ex){
        String detail=ex.getHeaderName()+" 헤더가 필요합니다.";
        log.warn("필수 헤더 누락: {}", ex.getHeaderName());
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(new ErrorBody(ErrorCode.INVALID_INPUT.name(), detail));
    }
}
