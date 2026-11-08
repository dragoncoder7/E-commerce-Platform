package com.ecommerce.common.core.constant;

/**
 * 返回码常量
 */
public class ResultCode {

    /**
     * 成功
     */
    public static final int SUCCESS = 200;

    /**
     * 业务异常
     */
    public static final int BUSINESS_ERROR = 400;

    /**
     * 未认证
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 无权限
     */
    public static final int FORBIDDEN = 403;

    /**
     * 资源不存在
     */
    public static final int NOT_FOUND = 404;

    /**
     * 服务器错误
     */
    public static final int INTERNAL_SERVER_ERROR = 500;

    /**
     * 服务不可用
     */
    public static final int SERVICE_UNAVAILABLE = 503;

    private ResultCode() {}
}
