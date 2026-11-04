package com.yilian.madp.auth.security;


import com.yilian.madp.auth.entity.SysUser;
import com.yilian.madp.auth.mapper.AuthMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthMapper authMapper;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization != null
                && authorization.startsWith("Bearer ")) {

            String token = authorization.substring(7);

            try {
                Long userId = jwtService.getUserId(token);
                String tokenRole = jwtService.getRole(token);

                // JWT 是无状态凭证，但管理员禁用账号后，旧 Token 也必须立即失效。
                // 因此每次请求都重新确认账号存在且状态正常，并使用数据库中的最新角色。
                SysUser user = authMapper.selectById(userId);
                if (user == null || !"正常".equals(user.getStatus())) {
                    throw new IllegalStateException("账号不存在或已禁用");
                }
                String role = user.getRole();
                if (!role.equalsIgnoreCase(tokenRole)) {
                    throw new IllegalStateException("用户角色已经变化");
                }

                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority(
                                "ROLE_" + role.toUpperCase()
                        );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(authority)
                        );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);

            } catch (Exception exception) {
                // Token 无效时不设置登录状态，
                // 后续由 Spring Security 返回 401。
            }
        }

        filterChain.doFilter(request, response);
    }
}
