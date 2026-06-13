package com.hd.rag.types.exception;

import com.hd.rag.types.errorcode.BaseErrorCode;
import com.hd.rag.types.errorcode.ErrorCode;


/**
 * 服务端运行异常
 * 请求运行过程中出现的不符合业务预期的异常
 */
public class ServiceException extends AbstractException {

    public ServiceException(String message) {
        this(message, null, BaseErrorCode.SERVICE_ERROR);
    }

    public ServiceException(ErrorCode errorCode) {
        this(null, errorCode);
    }

    public ServiceException(String message, ErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public ServiceException(String message, Throwable throwable, ErrorCode errorCode) {
        super(message, throwable, errorCode);
    }

    @Override
    public String toString() {
        return "ServiceException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
