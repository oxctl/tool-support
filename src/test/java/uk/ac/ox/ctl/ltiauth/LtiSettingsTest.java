package uk.ac.ox.ctl.ltiauth;

import org.junit.jupiter.api.Test;
import uk.ac.ox.ctl.ltiauth.LtiSettings;

import static org.junit.jupiter.api.Assertions.*;

class LtiSettingsTest {
    
    @Test
    public void testNoSettings() {
        // Check that when we don't have any settings we don't blow up with a NPE.
        LtiSettings settings = new LtiSettings(null, false, null, null);
        assertNull(settings.getClientSettings("test"));
    }

}