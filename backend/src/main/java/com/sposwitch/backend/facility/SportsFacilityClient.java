package com.sposwitch.backend.facility;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Pulls every Seoul facility from the 국민체육진흥공단 전국체육시설 API (about 23 pages of 1,000). */
@Component
public class SportsFacilityClient {

    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 100;
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .build();

    private final HttpClient httpClient;
    private final SportsFacilityApiProperties properties;

    public SportsFacilityClient(SportsFacilityApiProperties properties) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(7))
                .build();
        this.properties = properties;
    }

    public List<Facility> fetchSeoul() {
        List<Facility> facilities = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            Body body = parse(fetchPage(page));
            List<Item> items = body.items() == null || body.items().item() == null ? List.of() : body.items().item();
            items.stream().map(Facility::from).filter(Objects::nonNull).forEach(facilities::add);
            if (items.size() < PAGE_SIZE || (long) page * PAGE_SIZE >= body.totalCount()) {
                return facilities;
            }
        }
        throw new ExternalApiException("전국체육시설 API 페이지 수가 예상보다 많습니다.");
    }

    private String fetchPage(int page) {
        try {
            URI uri = URI.create(properties.baseUrl() + "/TODZ_API_SFMS_FACI?" + String.join("&",
                    queryParameter("serviceKey", decodedServiceKey()),
                    queryParameter("pageNo", Integer.toString(page)),
                    queryParameter("numOfRows", Integer.toString(PAGE_SIZE)),
                    queryParameter("resultType", "json"),
                    queryParameter("cp_nm", "서울특별시")
            ));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 403) {
                throw new ExternalApiException("전국체육시설 API 사용 권한이 없습니다. data.go.kr 활용신청 승인 여부를 확인하세요.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ExternalApiException("전국체육시설 API가 HTTP " + response.statusCode() + "을 반환했습니다.");
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException("전국체육시설 API 호출이 중단되었습니다.", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new ExternalApiException("전국체육시설 API 호출에 실패했습니다.", exception);
        }
    }

    static Body parse(String json) {
        FacilityResponse response;
        try {
            response = JSON_MAPPER.readValue(json, FacilityResponse.class);
        } catch (RuntimeException exception) {
            throw new ExternalApiException("전국체육시설 API 응답 형식이 올바르지 않습니다.", exception);
        }
        if (response == null || response.response() == null || response.response().header() == null) {
            throw new ExternalApiException("전국체육시설 API 응답 형식이 올바르지 않습니다.");
        }
        Header header = response.response().header();
        if (!"00".equals(header.resultCode())) {
            throw new ExternalApiException("전국체육시설 API 오류: " + header.resultMsg());
        }
        return response.response().body() == null ? new Body(0, null) : response.response().body();
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

    record FacilityResponse(Response response) {
    }

    record Response(Header header, Body body) {
    }

    record Header(String resultCode, String resultMsg) {
    }

    record Body(long totalCount, Items items) {
    }

    record Items(List<Item> item) {
    }

    /** Field names mirror the API; everything arrives as text. */
    record Item(
            String faci_cd,
            String faci_nm,
            String faci_stat_nm,
            String ftype_nm,
            String fcob_nm,
            String inout_gbn_nm,
            String faci_gb_nm,
            String fmng_cp_nm,
            String fmng_cpb_nm,
            String faci_mng_type_cd,
            String faci_road_addr,
            String faci_addr,
            String faci_tel_no,
            String faci_lat,
            String faci_lot,
            String updt_dt
    ) {
    }
}
