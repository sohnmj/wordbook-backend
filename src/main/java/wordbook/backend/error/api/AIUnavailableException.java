package wordbook.backend.error.api;

import wordbook.backend.error.CustomException;
import wordbook.backend.error.ErrorCode;

public class AIUnavailableException extends CustomException {
    public AIUnavailableException(Throwable cause){
        super(ErrorCode.AI_UNAVAILABLE,cause);
    }
}
