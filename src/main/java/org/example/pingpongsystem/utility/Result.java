package org.example.pingpongsystem.utility;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Result<T> {
    // getter和setter
    // 状态码
    private int code;
    // 提示信息
    private String message;
    // 业务数据
    private T data;

    // 构造方法
    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 静态工厂方法 - 成功并返回数据
    public static <T> Result<T> success(T data) {
        return new Result<>(StatusCode.SUCCESS, "操作成功", data);
    }

    // 静态工厂方法 - 仅返回状态
    public static <T> Result<T> success() {
        return new Result<>(StatusCode.SUCCESS, "操作成功", null);
    }

    // 静态工厂方法 - 错误状态
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccess() {return code == StatusCode.SUCCESS;}

}