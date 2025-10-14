package uk.ac.ox.ctl.canvasproxy;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Adds on a Via header to indicate that the request passed through this proxy.
 * This allows for easier debugging of requests. This is done in a filter as it's dealing with low-level concerns and
 * doesn't fit well in the controller.
 */
public class ViaFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest request && servletResponse instanceof HttpServletResponse response) {
            String existingVia = response.getHeader("Via");
            String newVia = request.getProtocol() + " " + request.getServerName() + " tool-support";
            if (existingVia != null && !existingVia.isEmpty()) {
                newVia = existingVia + ", " + newVia;
            }
            response.setHeader("Via", newVia);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
