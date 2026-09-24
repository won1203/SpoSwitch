package com.sposwitch.backend.environment.service;

import com.sposwitch.backend.common.error.ExternalApiException;
import com.sposwitch.backend.environment.client.AirKoreaClient;
import com.sposwitch.backend.environment.client.AirKoreaClient.AirQuality;
import com.sposwitch.backend.environment.client.KakaoLocalClient;
import com.sposwitch.backend.environment.client.KmaWeatherClient;
import com.sposwitch.backend.environment.client.KmaWeatherClient.Forecast;
import com.sposwitch.backend.environment.weather.KmaGridConverter;
import com.sposwitch.backend.environment.weather.KmaGridConverter.GridPoint;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final KakaoLocalClient kakaoLocalClient;
    private final KmaWeatherClient kmaWeatherClient;
    private final AirKoreaClient airKoreaClient;
    private final KmaGridConverter gridConverter;

    public WeatherService(
            KakaoLocalClient kakaoLocalClient,
            KmaWeatherClient kmaWeatherClient,
            AirKoreaClient airKoreaClient,
            KmaGridConverter gridConverter
    ) {
        this.kakaoLocalClient = kakaoLocalClient;
        this.kmaWeatherClient = kmaWeatherClient;
        this.airKoreaClient = airKoreaClient;
        this.gridConverter = gridConverter;
    }

    public CurrentWeather getCurrentWeather(double latitude, double longitude) {
        GridPoint gridPoint = gridConverter.convert(latitude, longitude);
        String location = kakaoLocalClient.findAdministrativeRegion(latitude, longitude);
        Forecast forecast = kmaWeatherClient.fetchUltraShortForecast(gridPoint);

        return new CurrentWeather(
                location,
                latitude,
                longitude,
                forecast.temperatureC(),
                forecast.condition(),
                forecast.conditionCode(),
                forecast.humidityPercent(),
                forecast.windSpeedMps(),
                forecast.precipitationAmount(),
                forecast.forecastAt(),
                "카카오 로컬 · 기상청 초단기예보 · 에어코리아",
                findAirQuality(location)
        );
    }

    /** Air quality is optional: a failure here must not hide the weather, and missing stays null. */
    private AirQuality findAirQuality(String location) {
        String[] parts = location.split(" ");
        if (parts.length < 2 || !"서울특별시".equals(parts[0])) {
            return null;
        }
        // ponytail: matches the 구 name to Seoul's same-named urban station.
        // Switch to nearest-station lookup (측정소정보 API) if coverage outside Seoul is needed.
        try {
            return airKoreaClient.findByStation(parts[1]).orElse(null);
        } catch (ExternalApiException exception) {
            log.warn("에어코리아 조회 실패: {}", exception.getMessage(), exception);
            return null;
        }
    }

    public record CurrentWeather(
            String location,
            double latitude,
            double longitude,
            double temperatureC,
            String condition,
            String conditionCode,
            int humidityPercent,
            double windSpeedMps,
            String precipitationAmount,
            OffsetDateTime forecastAt,
            String source,
            AirQuality airQuality
    ) {
    }
}
