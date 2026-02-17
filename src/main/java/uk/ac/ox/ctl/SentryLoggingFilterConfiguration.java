package uk.ac.ox.ctl;

import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentryLoggingFilterConfiguration {

	private static final String EXCLUDED_LOGGER =
			"org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver";

	@Bean
	public SentryOptions.BeforeSendCallback sentryBeforeSendCallback() {
		return new SentryOptions.BeforeSendCallback() {
			@Override
			public SentryEvent execute(SentryEvent event, Hint hint) {
				if (EXCLUDED_LOGGER.equals(event.getLogger())) {
					return null;
				}
				return event;
			}
		};
	}
}
