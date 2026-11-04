package com.yilian.madp.common;
//统一返回集  泛型类
// 那些字段   有什么方法

public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }


//静态工厂方法，用来快速创建统一返回对象。
//    第一个默认成功方法
    public static <T> Result<T>  success(T date){
        return new Result<>(200,"操作成功",date);
    }
//自定义成功方法
    public static <T> Result<T> success(String message,T date){

        return new Result<>(200,message,date);
    }
//失败的方法
    public static <T> Result<T> error(Integer code ,String message){
        return new Result<>(code,message,null);
    }

}
