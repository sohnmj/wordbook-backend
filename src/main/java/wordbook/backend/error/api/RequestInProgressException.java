package wordbook.backend.error.api;

import wordbook.backend.error.CustomException;
import wordbook.backend.error.ErrorCode;

public class RequestInProgressException extends CustomException {
    public RequestInProgressException(){
        super(ErrorCode.REQUEST_IN_PROGRESS);
    }
}
