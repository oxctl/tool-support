package uk.ac.ox.ctl.ltiauth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;

/**
 * This will hold a JWT while the client gets redirected from the login service to the frontend.
 * Then the frontend will request it's JWT (using it's unique ID) and we can remove the JWT from our store.
 */
public class JWTStore {

    private final Logger log = LoggerFactory.getLogger(JWTStore.class);
    private Cache<String, Object> store;

    public JWTStore(Duration duration) {
        log.debug("JWT Store created and expiring after {}", duration);
        store = CacheBuilder.newBuilder()
                .expireAfterAccess(duration)
                .build();
    }

    /**
     * This stores a token and returns a secure ID that is unguessable.
     * @param token The token to store.
     * @return The ID.
     */
    public String store(Object token) {
        String uuid = UUID.randomUUID().toString();
        store.put(uuid, token);
        return uuid;
    }

    /**
     * Gets the JWT and removes it. Technically 2 threads could get the token at the same time, but to exploit this
     * an attacker would have to be as fast as the main request and then we've lost anyway. Removing the token is mainly
     * to prevent someone see a browser history and being able to get a token that allows  them to login.
     * @param key The ID.
     * @return The stored JWT.
     */
    public Object retrieve(String key) {
        Object value = store.getIfPresent(key);
        store.invalidate(key);
        return value;
    }
}
