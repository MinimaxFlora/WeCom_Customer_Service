package xlike.top.kn_ai_chat.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuthInterceptorTest {

    private final AdminAuthInterceptor interceptor = new AdminAuthInterceptor();

    @Test
    void rejectsAnonymousAdminApiWith401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/api/enterprise");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void redirectsAnonymousPageToLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/enterprise");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/login");
    }

    @Test
    void allowsLoggedInAdmin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/api/enterprise");
        request.getSession().setAttribute("isAdmin", true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }
}
