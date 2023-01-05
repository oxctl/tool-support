package uk.ac.ox.ctl.ltiauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ox.ctl.ltiauth.JWTStore;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class JWTStoreTest {

    private JWTStore jwtStore;

    @BeforeEach
    public void setUp() {
        this.jwtStore = new JWTStore(Duration.ofDays(1));
    }

    @Test
    public void testRoundTrip() {
        Object obj = new Object();
        String id = jwtStore.store(obj);
        assertNotNull(id);
        assertNotEquals("", id);

        Object retrieved = jwtStore.retrieve(id);
        assertNotNull(retrieved);
        assertEquals(obj, retrieved);
    }

    @Test
    public void testStoreSame() {
        Object obj = new Object();
        String first = jwtStore.store(obj);
        String second = jwtStore.store(obj);
        assertNotEquals(first, second);

        Object retrievedFirst = jwtStore.retrieve(first);
        Object retrievedSecond = jwtStore.retrieve(second);

        assertEquals(obj, retrievedFirst);
        assertEquals(obj, retrievedSecond);
    }

    @Test
    public void testStoreSingleRetrieve() {
        Object obj = new Object();
        String id = jwtStore.store(obj);
        Object retrieved = jwtStore.retrieve(id);
        assertNotNull(retrieved);
        Object retrievedAgain = jwtStore.retrieve(id);
        assertNull(retrievedAgain);
    }

    @Test
    public void testMissingItem() {
        assertNull(jwtStore.retrieve("nothing"));
    }

    @Test
    public void testExipry() {
        // Make sure things expire very soon
        jwtStore = new JWTStore(Duration.ofNanos(0));
        Object obj = new Object();
        String id = jwtStore.store(obj);
        Object retrieved = jwtStore.retrieve(id);
        assertNull(retrieved);
    }

}