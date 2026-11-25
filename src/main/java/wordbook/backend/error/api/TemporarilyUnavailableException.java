package wordbook.backend.error.api;

import wordbook.backend.error.CustomException;
import wordbook.backend.error.ErrorCode;

public class TemporarilyUnavailableException extends CustomException {
    public TemporarilyUnavailableException(Throwable cause){
        super(ErrorCode.TEMPORARILY_UNAVAILABLE,cause);
    }
}
