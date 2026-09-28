package com.sposwitch.backend.environment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sposwitch.backend.environment.client.AirKoreaClient;
import com.sposwitch.backend.common.error.ExternalApiException;
import com.sposwitch.backend.environment.client.KakaoLocalClient;
import com.sposwitch.backend.environment.client.KmaWeatherClient;
import com.sposwitch.backend.environment.client.KmaWeatherClient.Forecast;
import com.sposwitch.backend.environment.weather.KmaGridConverter;
import com.sposwitch.backend.environment.weather.KmaGridConverter.GridPoint;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class WeatherServiceTests {

    @Test
    void locationLookupDoesNotRequireWeatherApi() {
        KakaoLocalClient kakao = mock(KakaoLocalClient.class);
        KmaWeatherClient kma = mock(KmaWeatherClient.class);
        AirKoreaClient air = mock(AirKoreaClient.class);
        when(kakao.findAdministrativeRegion(37.5665, 126.9780)).thenReturn("서울특별시 중구 명동");

        WeatherService service = new WeatherService(kakao, kma, air, mock(KmaGridConverter.class));

        assertEquals("서울특별시 중구 명동", service.getCurrentLocation(37.5665, 126.9780).location());
        verifyNoInteractions(kma, air);
    }

    @Test
    void airKoreaFailureStillReturnsForecastWithoutAirKoreaAttribution() {
        KakaoLocalClient kakao = mock(KakaoLocalClient.class);
        KmaWeatherClient kma = mock(KmaWeatherClient.class);
        AirKoreaClient air = mock(AirKoreaClient.class);
        KmaGridConverter grid = mock(KmaGridConverter.class);
        GridPoint gridPoint = new GridPoint(60, 127);
        when(grid.convert(37.5665, 126.9780)).thenReturn(gridPoint);
        when(kakao.findAdministrativeRegion(37.5665, 126.9780)).thenReturn("서울특별시 중구 명동");
        when(kma.fetchUltraShortForecast(gridPoint)).thenReturn(new Forecast(
                22.0, 60, 1.5, "0mm", "PARTLY_CLOUDY", "구름많음", OffsetDateTime.now()
        ));
        when(air.findByStation("중구")).thenThrow(new ExternalApiException("에어코리아 API 사용 권한이 없습니다."));

        WeatherService.CurrentWeather weather = new WeatherService(kakao, kma, air, grid)
                .getCurrentWeather(37.5665, 126.9780);

        assertEquals("서울특별시 중구 명동", weather.location());
        assertEquals(22.0, weather.temperatureC());
        assertNull(weather.airQuality());
        assertEquals("카카오 로컬 · 기상청 초단기예보", weather.source());
    }
}
