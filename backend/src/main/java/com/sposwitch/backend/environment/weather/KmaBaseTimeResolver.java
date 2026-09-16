package com.sposwitch.backend.environment.weather;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class KmaBaseTimeResolver {

    static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

    private final Clock clock;

    public KmaBaseTimeResolver() {
        this(Clock.system(KOREA_ZONE));
    }

    KmaBaseTimeResolver(Clock clock) {
        this.clock = clock;
    }

    public BaseTime latestPublishedUltraShortForecast() {
        LocalDateTime now = LocalDateTime.now(clock).withSecond(0).withNano(0);
        LocalDateTime base = now.getMinute() >= 45
                ? now.withMinute(30)
                : now.minusHours(1).withMinute(30);
        return new BaseTime(base.format(DATE_FORMAT), base.format(TIME_FORMAT));
    }

    public record BaseTime(String date, String time) {
    }
}
