package com.sposwitch.backend.environment.client;

import com.sposwitch.backend.common.config.AirKoreaApiProperties;
import com.sposwitch.backend.common.error.ExternalApiException;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AirKoreaClient {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    // ponytail: one in-memory cache for the whole Seoul response; stations update hourly and the dev quota is small.
    // Replace with the environment_cache design (D-06) once TTL policy is decided.
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final HttpClient httpClient;
    private final AirKoreaApiProperties properties;
    private List<Item> cachedItems;
    private Instant cachedAt = Instant.MIN;

    public AirKoreaClient(AirKoreaApiProperties properties) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.properties = properties;
    }

    public Optional<AirQuality> findByStation(String stationName) {
        return seoulStations().stream()
                .filter(item -> stationName.equals(item.stationName()))
                .findFirst()
                .map(AirKoreaClient::toAirQuality);
    }

    private synchronized List<Item> seoulStations() {
        if (cachedItems != null && Instant.now().isBefore(cachedAt.plus(CACHE_TTL))) {
            return cachedItems;
        }
        cachedItems = parse(fetchSeoul());
        cachedAt = Instant.now();
        return cachedItems;
    }

    /** The first call after startup can be slow (DNS + TLS), so one timeout is retried before giving up. */
    private String fetchSeoul() {
        try {
            return requestSeoul();
        } catch (IOException firstFailure) {
            try {
                return requestSeoul();
            } catch (IOException retryFailure) {
                throw new ExternalApiException("에어코리아 API 호출에 실패했습니다.", retryFailure);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ExternalApiException("에어코리아 API 호출이 중단되었습니다.", exception);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException("에어코리아 API 호출이 중단되었습니다.", exception);
        }
    }

    /** Network failures propagate so the caller can retry; bad responses fail straight away. */
    private String requestSeoul() throws IOException, InterruptedException {
        URI uri;
        try {
            uri = URI.create(properties.baseUrl() + "/getCtprvnRltmMesureDnsty?" + String.join("&",
                    queryParameter("serviceKey", decodedServiceKey()),
                    queryParameter("returnType", "json"),
                    queryParameter("numOfRows", "100"),
                    queryParameter("pageNo", "1"),
                    queryParameter("sidoName", "서울"),
                    queryParameter("ver", "1.3")
            ));
        } catch (IllegalArgumentException exception) {
            throw new ExternalApiException("에어코리아 API 주소를 만들 수 없습니다.", exception);
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 403) {
            throw new ExternalApiException("에어코리아 API 사용 권한이 없습니다. data.go.kr 활용신청 승인 여부를 확인하세요.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ExternalApiException("에어코리아 API가 HTTP " + response.statusCode() + "을 반환했습니다.");
        }
        return response.body();
    }

    static List<Item> parse(String body) {
        AirKoreaResponse response;
        try {
            response = JSON_MAPPER.readValue(body, AirKoreaResponse.class);
        } catch (RuntimeException exception) {
            throw new ExternalApiException("에어코리아 API 응답 형식이 올바르지 않습니다.", exception);
        }
        if (response == null || response.response() == null || response.response().header() == null) {
            throw new ExternalApiException("에어코리아 API 응답 형식이 올바르지 않습니다.");
        }
        Header header = response.response().header();
        if (!"00".equals(header.resultCode())) {
            throw new ExternalApiException("에어코리아 API 오류: " + header.resultMsg());
        }
        Body responseBody = response.response().body();
        return responseBody == null || responseBody.items() == null ? List.of() : responseBody.items();
    }

    static AirQuality toAirQuality(Item item) {
        return new AirQuality(
                item.stationName(),
                number(item.pm10Value()),
                grade(item.pm10Grade1h()),
                number(item.pm25Value()),
                grade(item.pm25Grade1h()),
                measuredAt(item.dataTime())
        );
    }

    /** Missing or inspection values ("-", blank) stay null; never coerce them to 0. */
    private static Integer number(String value) {
        if (value == null) return null;
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer grade(String value) {
        Integer grade = number(value);
        return grade != null && grade >= 1 && grade <= 4 ? grade : null;
    }

    /** AirKorea reports midnight as "yyyy-MM-dd 24:00". */
    private static OffsetDateTime measuredAt(String dataTime) {
        if (dataTime == null || dataTime.length() != 16) return null;
        try {
            LocalDate date = LocalDate.parse(dataTime.substring(0, 10));
            String time = dataTime.substring(11);
            if ("24:00".equals(time)) {
                return date.plusDays(1).atStartOfDay(KOREA_ZONE).toOffsetDateTime();
            }
            return date.atTime(LocalTime.parse(time)).atZone(KOREA_ZONE).toOffsetDateTime();
        } catch (RuntimeException exception) {
            return null;
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

    /** Grades follow AirKorea: 1 좋음, 2 보통, 3 나쁨, 4 매우나쁨. Null means missing. */
    public record AirQuality(
            String stationName,
            Integer pm10,
            Integer pm10Grade,
            Integer pm25,
            Integer pm25Grade,
            OffsetDateTime measuredAt
    ) {
    }

    record AirKoreaResponse(Response response) {
    }

    record Response(Header header, Body body) {
    }

    record Header(String resultCode, String resultMsg) {
    }

    record Body(int totalCount, List<Item> items) {
    }

    record Item(
            String stationName,
            String dataTime,
            String pm10Value,
            String pm25Value,
            String pm10Grade1h,
            String pm25Grade1h
    ) {
    }
}
