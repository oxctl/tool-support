package uk.ac.ox.ctl.canvasproxy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.*;

class ViaFilterTest {

    private ViaFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new ViaFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    void shouldAddViaHeaderWhenNoneExists() throws Exception {
        when(request.getProtocol()).thenReturn("HTTP/1.1");
        when(request.getServerName()).thenReturn("localhost");
        when(response.getHeader("Via")).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response).setHeader("Via", "HTTP/1.1 localhost tool-support");
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldAppendToExistingViaHeader() throws Exception {
        when(request.getProtocol()).thenReturn("HTTP/2");
        when(request.getServerName()).thenReturn("myserver");
        when(response.getHeader("Via")).thenReturn("HTTP/1.1 proxy1");

        filter.doFilter(request, response, chain);

        verify(response).setHeader("Via", "HTTP/1.1 proxy1, HTTP/2 myserver tool-support");
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldHandleNonHttpRequestsGracefully() throws IOException, jakarta.servlet.ServletException {
        ServletRequest genericRequest = mock(ServletRequest.class);
        ServletResponse genericResponse = mock(ServletResponse.class);

        filter.doFilter(genericRequest, genericResponse, chain);

        verify(chain).doFilter(genericRequest, genericResponse);
        // no headers should be set
        verifyNoInteractions(response);
    }
}