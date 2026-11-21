package wordbook.backend.error;

//공통 에러 응답 바디. code는 ErrorCode 이름(예: USAGE_EXHAUSTED)으로, 프론트가 분기하는 기준이다
public record ErrorBody(String code, String message) {}
