package xlike.top.kn_ai_chat.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 单管理员模式下的统一后台鉴权，替代各 Controller 自己判断 session 的碎片化做法。
 */
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return true;
        }

        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        boolean apiRequest = uri.startsWith("/api/")
                || uri.startsWith("/admin/api/")
                || (accept != null && accept.contains("application/json"));
        if (apiRequest) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"未授权访问\"}");
        } else {
            response.sendRedirect("/admin/login");
        }
        return false;
    }
}
