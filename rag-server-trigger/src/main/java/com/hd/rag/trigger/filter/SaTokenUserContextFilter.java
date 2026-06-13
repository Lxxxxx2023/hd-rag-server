package com.hd.rag.trigger.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.hd.rag.types.context.UserContext;
import com.hd.rag.types.context.UserContextHolder;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Sa-Token 用户上下文过滤器
 * <p>
 * 在请求进入时从 Sa-Token 提取当前用户并设入 {@link UserContextHolder}，
 * 领域层通过 {@link UserContextHolder#get()} 即可获取当前用户，无需直接依赖 StpUtil。
 * </p>
 */
@Component
@Order(1)
public class SaTokenUserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (StpUtil.isLogin()) {
                String userId = StpUtil.getLoginIdAsString();
                // Sa-Token 默认 loginId 是 userId，username 需额外获取
                // 此处 username 暂用 userId 填充，后续可通过 token session 扩展
                UserContextHolder.set(new UserContext(userId, userId));
            }
            // 未登录时不设置，UserContextHolder.get() 会返回 SYSTEM
            chain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }
}
