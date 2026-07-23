package com.orientsec.idap.common.model;

import lombok.extern.slf4j.Slf4j;

/**
 * 响应结果生成工具
 */
@SuppressWarnings({"rawtypes","unchecked"})
@Slf4j
public class ResultGenerator {
    private static final String DEFAULT_SUCCESS_MESSAGE = "SUCCESS";

    public static Result genSuccessResult() {
        return new Result()
                .setCode(ResultCode.SUCCESS)
                .setMessage(DEFAULT_SUCCESS_MESSAGE);
    }

    public static <T> Result<T> genSuccessResult(T data) {
        return new Result()
                .setCode(ResultCode.SUCCESS)
                .setMessage(DEFAULT_SUCCESS_MESSAGE)
                .setData(data);
    }

    public static Result genFailResult(Exception e) {
        String msg = null;
        if(e !=null){
            log.error(e.getMessage(),e);
            msg = e.getMessage();
        }
        return new Result()
                .setCode(ResultCode.FAIL)
                .setMessage(msg);
    }
}
