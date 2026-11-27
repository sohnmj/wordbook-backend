package wordbook.backend.redis;

public record IdempotencyResult(IdempotencyStatus status, String responseJson) {

    public static IdempotencyResult started() {
        return new IdempotencyResult(IdempotencyStatus.STARTED, null);
    }

    public static IdempotencyResult inProgress() {
        return new IdempotencyResult(IdempotencyStatus.IN_PROGRESS, null);
    }

    public static IdempotencyResult done(String responseJson) {
        return new IdempotencyResult(IdempotencyStatus.DONE, responseJson);
    }
}