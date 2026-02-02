package com.tcc.streaming.stream.core.exceptions;

import com.tcc.streaming.common.core.exceptions.BusinessException;

public class InvalidStreamStateException extends BusinessException {
    public InvalidStreamStateException(String message) {
        super(message);
    }
}
