package com.yilian.madp.auth.service;

import com.yilian.madp.auth.vo.CurrentUserResponse;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
//没看
/**
 * 从 Spring Security 上下文中读取当前登录用户。
 *
 * JWT 过滤器校验 Token 后，会把用户 ID 和角色放进 SecurityContext；
 * 后续业务代码不需要再次解析 Token，只需要通过这个服务获取登录信息。
 */
@Service
public class CurrentUserService {

    public CurrentUserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = Long.valueOf(authentication.getPrincipal().toString());
        String role = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()).toLowerCase())
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        return new CurrentUserResponse(userId, role);
    }
}
