package uk.ac.ox.ctl.ltiauth;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class LtiSettingsTest {

    @Test
    public void testSettings() {
        LtiSettings settings = new LtiSettings(Duration.ofMinutes(60), "issuer");
        assertEquals(settings.getExpiration(), Duration.ofMinutes(60));
        assertEquals(settings.getIssuer(), "issuer");
    }

    @Test
    public void testNoSettings() {
        // Check that when we don't have any settings we don't blow up with a NPE.
        LtiSettings settings = new LtiSettings(null, null);
        assertEquals(settings.getExpiration(), Duration.of(8, ChronoUnit.HOURS));
        assertEquals(settings.getIssuer(), "https://localhost");
    }

}