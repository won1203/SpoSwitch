package com.sposwitch.backend.environment.client;

import com.sposwitch.backend.common.config.KakaoApiProperties;
import com.sposwitch.backend.common.error.ExternalApiException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoLocalClient {

    private final RestClient restClient;

    public KakaoLocalClient(KakaoApiProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restApiKey())
                .build();
    }

    public String findAdministrativeRegion(double latitude, double longitude) {
        try {
            KakaoRegionResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/geo/coord2regioncode.json")
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("input_coord", "WGS84")
                            .build())
                    .retrieve()
                    .body(KakaoRegionResponse.class);

            if (response == null || response.documents() == null || response.documents().isEmpty()) {
                throw new ExternalApiException("현재 좌표의 행정동을 찾을 수 없습니다.");
            }

            return response.documents().stream()
                    .filter(document -> "H".equals(document.region_type()))
                    .findFirst()
                    .orElse(response.documents().getFirst())
                    .address_name();
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ExternalApiException("카카오 위치 API 호출에 실패했습니다.", exception);
        }
    }

    record KakaoRegionResponse(List<Document> documents) {
    }

    record Document(String region_type, String address_name) {
    }
}
