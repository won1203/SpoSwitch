package com.sposwitch.backend.environment.client;

import com.sposwitch.backend.common.config.KmaApiProperties;
import com.sposwitch.backend.common.error.ExternalApiException;
import com.sposwitch.backend.environment.weather.KmaBaseTimeResolver;
import com.sposwitch.backend.environment.weather.KmaGridConverter.GridPoint;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class KmaWeatherClient {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORECAST_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final KmaApiProperties properties;
    private final KmaBaseTimeResolver baseTimeResolver;

    public KmaWeatherClient(
            KmaApiProperties properties,
            KmaBaseTimeResolver baseTimeResolver
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(7))
                .build();
        this.jsonMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        this.properties = properties;
        this.baseTimeResolver = baseTimeResolver;
    }

    public Forecast fetchUltraShortForecast(GridPoint gridPoint) {
        KmaBaseTimeResolver.BaseTime baseTime = baseTimeResolver.latestPublishedUltraShortForecast();
        try {
            URI uri = URI.create(properties.baseUrl() + "/getUltraSrtFcst?" + String.join("&",
                    queryParameter("serviceKey", decodedServiceKey()),
                    queryParameter("pageNo", "1"),
                    queryParameter("numOfRows", "1000"),
                    queryParameter("dataType", "JSON"),
                    queryParameter("base_date", baseTime.date()),
                    queryParameter("base_time", baseTime.time()),
                    queryParameter("nx", Integer.toString(gridPoint.x())),
                    queryParameter("ny", Integer.toString(gridPoint.y()))
            ));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                if (httpResponse.statusCode() == 403
                        && httpResponse.body().contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR")) {
                    throw new ExternalApiException("기상청 API 인증키가 등록되지 않았습니다. 서버의 KMA_SERVICE_KEY와 단기예보 조회서비스 활용 승인을 확인해 주세요.");
                }
                throw new ExternalApiException("기상청 날씨 API가 HTTP " + httpResponse.statusCode() + "을 반환했습니다.");
            }
            KmaResponse response = jsonMapper.readValue(httpResponse.body(), KmaResponse.class);
            return mapForecast(response);
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException("기상청 날씨 API 호출이 중단되었습니다.", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new ExternalApiException("기상청 날씨 API 호출에 실패했습니다.", exception);
        }
    }

    private String queryParameter(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String decodedServiceKey() {
        String serviceKey = properties.serviceKey();
        return serviceKey.matches(".*%[0-9a-fA-F]{2}.*")
                ? URLDecoder.decode(serviceKey, StandardCharsets.UTF_8)
                : serviceKey;
    }

    private Forecast mapForecast(KmaResponse response) {
        if (response == null || response.response() == null || response.response().header() == null) {
            throw new ExternalApiException("기상청 날씨 API 응답 형식이 올바르지 않습니다.");
        }
        Header header = response.response().header();
        if (!"00".equals(header.resultCode())) {
            throw new ExternalApiException("기상청 날씨 API 오류: " + header.resultMsg());
        }
        if (response.response().body() == null
                || response.response().body().items() == null
                || response.response().body().items().item() == null) {
            throw new ExternalApiException("기상청 날씨 데이터가 비어 있습니다.");
        }

        Map<String, Map<String, String>> forecastsByTime = new HashMap<>();
        for (Item item : response.response().body().items().item()) {
            String key = item.fcstDate() + item.fcstTime();
            forecastsByTime.computeIfAbsent(key, ignored -> new HashMap<>())
                    .put(item.category(), item.fcstValue());
        }

        Map.Entry<String, Map<String, String>> selected = forecastsByTime.entrySet().stream()
                .filter(entry -> entry.getValue().containsKey("T1H"))
                .min(Comparator.comparing(Map.Entry::getKey))
                .orElseThrow(() -> new ExternalApiException("표시할 수 있는 기상청 예보가 없습니다."));

        Map<String, String> values = selected.getValue();
        String precipitationCode = values.getOrDefault("PTY", "0");
        String skyCode = values.getOrDefault("SKY", "1");
        WeatherCondition condition = WeatherCondition.from(precipitationCode, skyCode);
        LocalDateTime forecastDateTime = LocalDateTime.parse(selected.getKey(), FORECAST_FORMAT);
        OffsetDateTime forecastAt = forecastDateTime.atZone(KOREA_ZONE).toOffsetDateTime();

        return new Forecast(
                parseDouble(values.get("T1H"), "기온"),
                parseInteger(values.get("REH"), "습도"),
                parseDouble(values.get("WSD"), "풍속"),
                values.getOrDefault("RN1", "강수없음"),
                condition.code,
                condition.label,
                forecastAt
        );
    }

    private double parseDouble(String value, String category) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException exception) {
            throw new ExternalApiException("기상청 " + category + " 값을 해석할 수 없습니다.", exception);
        }
    }

    private int parseInteger(String value, String category) {
        return (int) Math.round(parseDouble(value, category));
    }

    private enum WeatherCondition {
        CLEAR("CLEAR", "맑음"),
        PARTLY_CLOUDY("PARTLY_CLOUDY", "구름많음"),
        CLOUDY("CLOUDY", "흐림"),
        RAIN("RAIN", "비"),
        RAIN_SNOW("RAIN_SNOW", "비 또는 눈"),
        SNOW("SNOW", "눈"),
        SHOWER("SHOWER", "소나기");

        private final String code;
        private final String label;

        WeatherCondition(String code, String label) {
            this.code = code;
            this.label = label;
        }

        static WeatherCondition from(String precipitationCode, String skyCode) {
            return switch (precipitationCode) {
                case "1", "5" -> RAIN;
                case "2", "6" -> RAIN_SNOW;
                case "3", "7" -> SNOW;
                case "4" -> SHOWER;
                default -> switch (skyCode) {
                    case "3" -> PARTLY_CLOUDY;
                    case "4" -> CLOUDY;
                    default -> CLEAR;
                };
            };
        }
    }

    public record Forecast(
            double temperatureC,
            int humidityPercent,
            double windSpeedMps,
            String precipitationAmount,
            String conditionCode,
            String condition,
            OffsetDateTime forecastAt
    ) {
    }

    record KmaResponse(Response response) {
    }

    record Response(Header header, Body body) {
    }

    record Header(String resultCode, String resultMsg) {
    }

    record Body(String dataType, int pageNo, int numOfRows, int totalCount, Items items) {
    }

    record Items(List<Item> item) {
    }

    record Item(
            String baseDate,
            String baseTime,
            String category,
            String fcstDate,
            String fcstTime,
            String fcstValue,
            int nx,
            int ny
    ) {
    }
}
