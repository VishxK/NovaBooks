package lk.vish.novabooks.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter("/api/books/*")
public class AdminSecurityFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if ("GET".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        boolean isAdmin = false;

        if (session != null) {
            String role = (String) session.getAttribute("role");
            if ("ADMIN".equals(role)) {
                isAdmin = true;
            }
        }

        if (isAdmin) {
            chain.doFilter(request, response);
        } else {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write("{\"success\":false, \"message\":\"Unauthorized: Admin access required to modify books.\"}");
        }
    }

    @Override
    public void destroy() {

    }
}
