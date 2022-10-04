package uk.ac.ox.ctl.canvasproxy;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.support.WebTestUtils;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is copied from Spring as the classes in Spring 
 */
public class AuthenticationRequestPostProcessor implements RequestPostProcessor {

    private final Authentication authentication;

    public AuthenticationRequestPostProcessor(Authentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(this.authentication);
        save(this.authentication, request);
        return request;
    }

    /**
     * Saves the specified {@link Authentication} into an empty
     * {@link SecurityContext} using the {@link SecurityContextRepository}.
     *
     * @param authentication the {@link Authentication} to save
     * @param request        the {@link HttpServletRequest} to use
     */
    final void save(Authentication authentication, HttpServletRequest request) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        save(securityContext, request);
    }

    /**
     * Saves the {@link SecurityContext} using the {@link SecurityContextRepository}
     *
     * @param securityContext the {@link SecurityContext} to save
     * @param request         the {@link HttpServletRequest} to use
     */
    final void save(SecurityContext securityContext, HttpServletRequest request) {
        SecurityContextRepository securityContextRepository = WebTestUtils.getSecurityContextRepository(request);
        boolean isTestRepository = securityContextRepository instanceof TestSecurityContextRepository;
        if (!isTestRepository) {
            securityContextRepository = new TestSecurityContextRepository(securityContextRepository);
            WebTestUtils.setSecurityContextRepository(request, securityContextRepository);
        }
        HttpServletResponse response = new MockHttpServletResponse();
        HttpRequestResponseHolder requestResponseHolder = new HttpRequestResponseHolder(request, response);
        securityContextRepository.loadContext(requestResponseHolder);
        request = requestResponseHolder.getRequest();
        response = requestResponseHolder.getResponse();
        securityContextRepository.saveContext(securityContext, request, response);
    }

    /**
     * Used to wrap the SecurityContextRepository to provide support for testing in
     * stateless mode
     */
    static final class TestSecurityContextRepository implements SecurityContextRepository {

        private static final String ATTR_NAME = TestSecurityContextRepository.class.getName().concat(".REPO");

        private final SecurityContextRepository delegate;

        private TestSecurityContextRepository(SecurityContextRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
            SecurityContext result = getContext(requestResponseHolder.getRequest());
            // always load from the delegate to ensure the request/response in the
            // holder are updated
            // remember the SecurityContextRepository is used in many different
            // locations
            SecurityContext delegateResult = this.delegate.loadContext(requestResponseHolder);
            return (result != null) ? result : delegateResult;
        }

        @Override
        public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
            request.setAttribute(ATTR_NAME, context);
            this.delegate.saveContext(context, request, response);
        }

        @Override
        public boolean containsContext(HttpServletRequest request) {
            return getContext(request) != null || this.delegate.containsContext(request);
        }

        private static SecurityContext getContext(HttpServletRequest request) {
            return (SecurityContext) request.getAttribute(ATTR_NAME);
        }

    }


}