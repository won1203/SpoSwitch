package com.sposwitch.backend.environment.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sposwitch.backend.common.error.ExternalApiException;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AirKoreaClientTests {

    private static final String SEOUL_RESPONSE = """
            {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
             "body":{"totalCount":2,"pageNo":1,"numOfRows":100,"items":[
               {"stationName":"양천구","dataTime":"2026-09-21 24:00","pm10Value":"23","pm25Value":"-",
                "pm10Grade1h":"1","pm25Grade1h":null,"pm25Flag":"통신장애"},
               {"stationName":"강서구","dataTime":"2026-09-22 02:00","pm10Value":"81","pm25Value":"40",
                "pm10Grade1h":"3","pm25Grade1h":"3"}]}}}
            """;

    @Test
    void keepsMissingValuesNullAndReadsMidnightAsNextDay() {
        List<AirKoreaClient.Item> items = AirKoreaClient.parse(SEOUL_RESPONSE);
        AirKoreaClient.AirQuality yangcheon = AirKoreaClient.toAirQuality(items.getFirst());

        assertEquals("양천구", yangcheon.stationName());
        assertEquals(23, yangcheon.pm10());
        assertEquals(1, yangcheon.pm10Grade());
        assertNull(yangcheon.pm25());
        assertNull(yangcheon.pm25Grade());
        assertEquals(OffsetDateTime.parse("2026-09-22T00:00+09:00"), yangcheon.measuredAt());

        AirKoreaClient.AirQuality gangseo = AirKoreaClient.toAirQuality(items.get(1));
        assertEquals(40, gangseo.pm25());
        assertEquals(3, gangseo.pm25Grade());
    }

    @Test
    void rejectsErrorHeader() {
        String error = """
                {"response":{"header":{"resultCode":"30","resultMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR"}}}
                """;

        assertThrows(ExternalApiException.class, () -> AirKoreaClient.parse(error));
    }
}
