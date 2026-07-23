package com.orientsec.idap.common.model;

/**
 *
 */
public enum ResultCode {
    SUCCESS(200),//成功
    FAIL(400),//失败
    UNAUTHORIZED(401),//未授权
    NOT_FOUND(404),//未找到
    INTERNAL_SERVER_ERROR(500);//服务器错误

    private final int code;

    ResultCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
