package uk.ac.ox.ctl;

import io.sentry.SentryEvent;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SentryLoggingFilterConfigurationTest {

	@Test
	void filterEvent_shouldReturnNull_whenLoggerIsDefaultHandlerExceptionResolver() {
		SentryEvent event = new SentryEvent();
		event.setLogger(DefaultHandlerExceptionResolver.class.getName());

		SentryEvent result = SentryLoggingFilterConfiguration.filterEvent(event);

		assertNull(result, "Event should be filtered (null) when logger is DefaultHandlerExceptionResolver");
	}

	@Test
	void filterEvent_shouldReturnEvent_whenLoggerIsNotExcluded() {
		SentryEvent event = new SentryEvent();
		event.setLogger("com.example.SomeOtherLogger");

		SentryEvent result = SentryLoggingFilterConfiguration.filterEvent(event);

		assertNotNull(result, "Event should not be filtered when logger is not excluded");
	}

	@Test
	void filterEvent_shouldReturnEvent_whenLoggerIsNull() {
		SentryEvent event = new SentryEvent();
		event.setLogger(null);

		SentryEvent result = SentryLoggingFilterConfiguration.filterEvent(event);

		assertNotNull(result, "Event should not be filtered when logger is null");
	}
}
