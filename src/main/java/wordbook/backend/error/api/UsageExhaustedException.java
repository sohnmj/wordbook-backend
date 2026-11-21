package wordbook.backend.error.api;

import wordbook.backend.error.CustomException;
import wordbook.backend.error.ErrorCode;

public class UsageExhaustedException extends CustomException {
    public UsageExhaustedException(){
        super(ErrorCode.USAGE_EXHAUSTED);
    }
}
