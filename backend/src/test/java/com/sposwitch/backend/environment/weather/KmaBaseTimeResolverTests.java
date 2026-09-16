package com.sposwitch.backend.environment.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class KmaBaseTimeResolverTests {

    @Test
    void usesCurrentHourAfterPublicationMinute() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-16T05:50:00Z"), KmaBaseTimeResolver.KOREA_ZONE);
        KmaBaseTimeResolver resolver = new KmaBaseTimeResolver(clock);

        KmaBaseTimeResolver.BaseTime result = resolver.latestPublishedUltraShortForecast();

        assertEquals("20260916", result.date());
        assertEquals("1430", result.time());
    }

    @Test
    void usesPreviousDayBeforeFirstPublicationIsAvailable() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-15T15:20:00Z"), KmaBaseTimeResolver.KOREA_ZONE);
        KmaBaseTimeResolver resolver = new KmaBaseTimeResolver(clock);

        KmaBaseTimeResolver.BaseTime result = resolver.latestPublishedUltraShortForecast();

        assertEquals("20260915", result.date());
        assertEquals("2330", result.time());
    }
}
