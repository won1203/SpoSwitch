package com.sposwitch.backend.environment.service;

import com.sposwitch.backend.environment.client.KakaoLocalClient;
import com.sposwitch.backend.environment.client.KmaWeatherClient;
import com.sposwitch.backend.environment.client.KmaWeatherClient.Forecast;
import com.sposwitch.backend.environment.weather.KmaGridConverter;
import com.sposwitch.backend.environment.weather.KmaGridConverter.GridPoint;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    private final KakaoLocalClient kakaoLocalClient;
    private final KmaWeatherClient kmaWeatherClient;
    private final KmaGridConverter gridConverter;

    public WeatherService(
            KakaoLocalClient kakaoLocalClient,
            KmaWeatherClient kmaWeatherClient,
            KmaGridConverter gridConverter
    ) {
        this.kakaoLocalClient = kakaoLocalClient;
        this.kmaWeatherClient = kmaWeatherClient;
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
                "카카오 로컬 · 기상청 초단기예보"
        );
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
            String source
    ) {
    }
}
