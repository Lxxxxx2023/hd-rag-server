package com.hd.rag.types.exception;

import com.hd.rag.types.errorcode.BaseErrorCode;
import com.hd.rag.types.errorcode.ErrorCode;

/**
 * 客户端异常
 * 用户发起调用请求后因客户端提交参数或其他客户端问题导致的异常
 */
public class ClientException extends AbstractException {

    public ClientException(ErrorCode errorCode) {
        this(null, null, errorCode);
    }

    public ClientException(String message) {
        this(message, null, BaseErrorCode.CLIENT_ERROR);
    }

    public ClientException(String message, ErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public ClientException(String message, Throwable throwable, ErrorCode errorCode) {
        super(java.util.Optional.ofNullable(message).orElse(errorCode.message()), throwable, errorCode);
    }

    @Override
    public String toString() {
        return "ClientException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
