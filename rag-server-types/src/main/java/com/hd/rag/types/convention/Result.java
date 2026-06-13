package com.hd.rag.types.convention;


import lombok.Data;
import lombok.experimental.Accessors;


@Data
@Accessors(chain = true)
public class Result <T>{

    /**
     * 成功状态码
     * <p>
     * 当接口请求成功时，返回此状态码
     * </p>
     */
    public static final String SUCCESS_CODE = "0";

    /**
     * 状态码
     * <p>
     * 标识请求的处理结果，{@code "0"} 表示成功，其他值表示各类错误或异常情况
     * </p>
     */
    private String code;

    /**
     * 响应消息
     * <p>
     * 对本次请求结果的文字描述，成功时可为成功提示，失败时为错误原因说明
     * </p>
     */
    private String message;

    /**
     * 响应数据
     * <p>
     * 接口返回的业务数据，类型由泛型 T 指定。请求失败时可能为 {@code null}
     * </p>
     */
    private T data;

    /**
     * 判断请求是否成功
     *
     * @return 如果状态码为 {@link #SUCCESS_CODE}，返回 {@code true}；否则返回 {@code false}
     */
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }

}
