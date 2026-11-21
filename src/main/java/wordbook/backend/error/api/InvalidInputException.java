package wordbook.backend.error.api;

import wordbook.backend.error.CustomException;
import wordbook.backend.error.ErrorCode;

public class InvalidInputException extends CustomException {
    public InvalidInputException() {
        super(ErrorCode.INVALID_INPUT);
    }
}
