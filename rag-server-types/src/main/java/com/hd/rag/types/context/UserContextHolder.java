package com.hd.rag.types.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 用户上下文持有者（TransmittableThreadLocal）
 * <p>
 * 由 Filter 在请求进入时设置，请求结束后清理。
 * 使用 TTL 确保异步任务（@Async / 线程池）中子线程也能获取到上下文。
 * 所有领域层通过 {@link #get()} 获取当前用户，无需依赖 Sa-Token / StpUtil。
 * </p>
 */
public final class UserContextHolder {

    private static final TransmittableThreadLocal<UserContext> CONTEXT = new TransmittableThreadLocal<>();

    private UserContextHolder() {
    }

    /** 设置当前请求的用户上下文 */
    public static void set(UserContext userContext) {
        CONTEXT.set(userContext);
    }

    /** 获取当前用户上下文，未登录时返回 {@link UserContext#SYSTEM} */
    public static UserContext get() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx : UserContext.SYSTEM;
    }

    /** 请求结束后清理，防止内存泄漏 */
    public static void clear() {
        CONTEXT.remove();
    }
}
