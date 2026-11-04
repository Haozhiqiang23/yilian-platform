package com.yilian.madp.config;

import com.yilian.madp.auth.security.JwtAuthenticationFilter;
import com.yilian.madp.auth.security.SecurityErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration
@RequiredArgsConstructor
/** SecurityConfig = 制定门禁规则
    JwtAuthenticationFilter = 执行门禁检查
    JwtService = 生成和解析通行证
* */
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorHandler securityErrorHandler;
    /*含义是向 Spring 注册一个密码加密器。以后 AuthService 需要 PasswordEncoder 时
    ，Spring 就把这个对象交给它。*/
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // 前后端分离项目使用 JWT，不使用传统 CSRF 防护。
                .csrf(csrf -> csrf.disable())

                // 不在服务器保存登录 Session。
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // 配置接口访问权限。

                /*匹配 /api/v1/auth/** 的接口 → 全部放行 其他所有接口→ 必须登录*/
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(
                                        "/api/v1/auth/**"
                                )
                                .permitAll()
                                // 患者业务接口只允许 patient 角色访问。
                                .requestMatchers("/api/v1/patients/**")
                                .hasRole("PATIENT")
                                // 预约业务只允许患者角色访问。
                                .requestMatchers("/api/v1/appointments/**")
                                .hasRole("PATIENT")
                                // 支付订单属于患者业务，只允许患者角色访问。
                                .requestMatchers("/api/v1/payments/**")
                                .hasRole("PATIENT")
                                // 签到属于患者业务，只允许患者角色访问。
                                .requestMatchers("/api/v1/sign-ins/**")
                                .hasRole("PATIENT")
                                // 患者排队状态仅允许患者角色查询。
                                .requestMatchers("/api/v1/queues/**")
                                .hasRole("PATIENT")
                                // 患者诊疗记录包含隐私信息，只允许患者本人访问。
                                .requestMatchers("/api/v1/medical-records/**")
                                .hasRole("PATIENT")
                                // 创建、编辑处方属于医生操作，必须放在通用处方规则之前。
                                .requestMatchers(HttpMethod.POST, "/api/v1/prescriptions")
                                .hasRole("DOCTOR")
                                .requestMatchers(HttpMethod.PUT, "/api/v1/prescriptions/**")
                                .hasRole("DOCTOR")
                                // 处方详情仅允许患者本人和开方医生进入接口。
                                // 具体数据归属由业务层继续校验。
                                .requestMatchers("/api/v1/prescriptions/**")
                                .hasAnyRole("PATIENT", "DOCTOR")
                                // 创建、录入检查结果属于医生操作，必须优先于通用检查规则。
                                .requestMatchers(HttpMethod.POST, "/api/v1/examinations")
                                .hasRole("DOCTOR")
                                .requestMatchers(HttpMethod.GET, "/api/v1/examinations/my")
                                .hasRole("PATIENT")
                                .requestMatchers(HttpMethod.PUT, "/api/v1/examinations/*/patient-result")
                                .hasRole("PATIENT")
                                .requestMatchers(HttpMethod.PUT, "/api/v1/examinations/**")
                                .hasRole("DOCTOR")
                                // 检查报告仅允许患者本人和接诊医生进入接口。
                                .requestMatchers("/api/v1/examinations/**")
                                .hasAnyRole("PATIENT", "DOCTOR")
                                // 站内消息属于患者隐私数据，只允许患者角色访问。
                                .requestMatchers("/api/v1/messages/**")
                                .hasRole("PATIENT")
                                // 医生工作台接口统一限制为 doctor 角色。
                                .requestMatchers("/api/v1/doctor/**")
                                .hasRole("DOCTOR")
                                // 管理员接口统一限制为 admin 角色；即使对应业务接口尚未实现，
                                // 其他角色访问也必须先经过权限校验并返回 403。
                                .requestMatchers("/api/v1/admin/**")
                                .hasRole("ADMIN")
                                .anyRequest()
                                .authenticated()
                )

                // 统一处理未登录（401）和权限不足（403）。
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler)
                )

                // JWT 过滤器放在用户名密码过滤器之前执行。
                /*Spring 处理请求之前 → 先运行 JwtAuthenticationFilter → 检查请求有没有合法 Token*/
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
