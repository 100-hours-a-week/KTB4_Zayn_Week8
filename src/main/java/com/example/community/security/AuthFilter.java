package com.example.community.security;

import com.example.community.common.ResponseFormat;
import com.example.community.common.ResponseMessage;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws IOException, ServletException {

        String authorization = request.getHeader("Authorization");

        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isH2ConsoleRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // login, join (GET, POST) 요청이면 통과
        if (isAuthPageRequest(request)) {
            handleAuthPageRequest(authorization, request, response, filterChain);
            return;
        }

        // token 재발급 요청이면 통과
        if (isTokenRefreshRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 이외 모든 요청에 유효한 토큰이 아니면 권한 없음 응답 반환
        if (!authenticateAccessToken(authorization)) {
            setUnauthorizedResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthPageRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        return ((method.equals("GET") || method.equals("POST"))
                && (path.equals("/login") || path.equals("/join")));
    }

    private void handleAuthPageRequest(String authorization, HttpServletRequest request,
                                       HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        if (authenticateAccessToken(authorization)) { // 유효한 토큰이 있는 경우
            setAuthorizedResponse(response); // /posts로
            return;
        }

        filterChain.doFilter(request, response); // 유효 토큰이 없는 경우, 필터 종료
    }

    private boolean isTokenRefreshRequest(HttpServletRequest request) {
        return request.getMethod().equals("POST") && request.getRequestURI().equals("/token");
    }

    private void setAuthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK); // 200
        response.setContentType("application/json;charset=UTF-8");

        ResponseFormat<Map<String, String>> responseBody = ResponseFormat.of(
                ResponseMessage.ALREADY_AUTHORIZED.getMessage(),
                Map.of("redirect_url", "/posts")
        );

        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }

    private void setUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");

        ResponseFormat<Map<String, String>> responseBody = ResponseFormat.of(
                ResponseMessage.UNAUTHORIZED.getMessage(),
                Map.of("redirect_url", "/login")
        );

        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }

    private boolean authenticateAccessToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }

        String token = authorization.substring(7);

        return tokenProvider.validateAccessToken(token);
    }

    private boolean isH2ConsoleRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/h2-console");
    }
}