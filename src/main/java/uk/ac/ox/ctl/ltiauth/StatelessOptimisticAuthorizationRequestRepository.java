package uk.ac.ox.ctl.ltiauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.OptimisticAuthorizationRequestRepository;

/**
 * Adapts the LTI library's optimistic repository to a cookie-free launch flow by delegating all persistence to a
 * short-lived state-keyed cache and never treating the browser as having a working session.
 */
public class StatelessOptimisticAuthorizationRequestRepository extends OptimisticAuthorizationRequestRepository {

    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> delegate;

    public StatelessOptimisticAuthorizationRequestRepository(AuthorizationRequestRepository<OAuth2AuthorizationRequest> delegate) {
        super(delegate, delegate);
        this.delegate = delegate;
    }

    @Override
    public boolean hasWorkingSession(HttpServletRequest request) {
        return false;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return delegate.loadAuthorizationRequest(request);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        delegate.saveAuthorizationRequest(authorizationRequest, request, response);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return delegate.removeAuthorizationRequest(request, response);
    }
}
