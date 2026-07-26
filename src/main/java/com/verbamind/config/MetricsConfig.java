package com.verbamind.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables the {@code @Timed} annotation across the app. Any method annotated with
 * {@code @Timed("some.metric.name")} gets its latency recorded automatically and
 * exposed at /actuator/metrics/some.metric.name (and /actuator/prometheus if scraped).
 */
@Configuration
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
