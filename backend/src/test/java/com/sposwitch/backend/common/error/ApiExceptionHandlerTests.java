package com.sposwitch.backend.common.error;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sposwitch.backend.environment.controller.WeatherController;
import com.sposwitch.backend.environment.service.WeatherService;
import com.sposwitch.backend.facility.FacilityCatalog;
import com.sposwitch.backend.facility.FacilityController;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest({WeatherController.class, FacilityController.class})
class ApiExceptionHandlerTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    WeatherService weatherService;

    @MockitoBean
    FacilityCatalog catalog;

    @Test
    void outOfRangeCoordinateIsBadRequest() throws Exception {
        mvc.perform(get("/api/v1/weather/current").param("latitude", "999").param("longitude", "127"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 값이 허용 범위를 벗어났습니다: latitude"));
    }

    @Test
    void wrongTypeDoesNotLeakJavaTypes() throws Exception {
        mvc.perform(get("/api/v1/weather/current").param("latitude", "abc").param("longitude", "127"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 값의 형식이 올바르지 않습니다: latitude"));
    }

    @Test
    void missingParameterNamesIt() throws Exception {
        mvc.perform(get("/api/v1/weather/current").param("latitude", "37.5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("필수 요청 값이 없습니다: longitude"));
    }

    @ParameterizedTest
    @MethodSource("invalidCoordinates")
    void invalidCoordinatesAreRejectedBeforeCallingServices(String path, String name, String value) throws Exception {
        mvc.perform(get(path)
                        .param("latitude", name.equals("latitude") ? value : "37.5")
                        .param("longitude", name.equals("longitude") ? value : "127"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("요청 값이 허용 범위를 벗어났습니다: " + name));
        verifyNoInteractions(weatherService, catalog);
    }

    static Stream<Arguments> invalidCoordinates() {
        return Stream.of("/api/v1/weather/current", "/api/v1/weather/location", "/api/v1/facilities", "/api/v1/facilities/ID")
                .flatMap(path -> Stream.of(
                        Arguments.of(path, "latitude", "90.1"),
                        Arguments.of(path, "latitude", "-90.1"),
                        Arguments.of(path, "longitude", "180.1"),
                        Arguments.of(path, "longitude", "-180.1"),
                        Arguments.of(path, "latitude", "NaN"),
                        Arguments.of(path, "longitude", "NaN"),
                        Arguments.of(path, "latitude", "Infinity"),
                        Arguments.of(path, "longitude", "-Infinity")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"latitude", "longitude"})
    void facilityDetailRequiresBothCoordinatesOrNeither(String name) throws Exception {
        mvc.perform(get("/api/v1/facilities/ID").param(name, "37.5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("거리 계산에는 latitude와 longitude를 함께 입력해야 합니다."));
        verifyNoInteractions(catalog);
    }

    @Test
    void loadingResponseKeepsAppErrorFormatAndSuggestsRetry() throws Exception {
        when(catalog.nearby(37.5, 127, null, null)).thenThrow(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "시설 정보를 불러오는 중입니다. 잠시 후 다시 시도해 주세요."));
        mvc.perform(get("/api/v1/facilities").param("latitude", "37.5").param("longitude", "127"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.message").value("시설 정보를 불러오는 중입니다. 잠시 후 다시 시도해 주세요."));
    }

    @Test
    void upstreamFailureIsBadGateway() throws Exception {
        when(catalog.nearby(37.5, 127, null, null)).thenThrow(new ExternalApiException("시설 API 호출 실패"));
        mvc.perform(get("/api/v1/facilities").param("latitude", "37.5").param("longitude", "127"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("시설 API 호출 실패"));
    }

    @Test
    void statusWithoutReasonStillReturnsJson() throws Exception {
        when(catalog.find("ID")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(get("/api/v1/facilities/ID"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Not Found"));
    }

    @Test
    void successfulNearbyResponseKeepsFieldsUsedByApp() throws Exception {
        when(catalog.nearby(37.5, 127, null, null)).thenReturn(
                new FacilityCatalog.Nearby(0, 3000, OffsetDateTime.parse("2026-09-30T09:00:00+09:00"), List.of()));
        mvc.perform(get("/api/v1/facilities").param("latitude", "37.5").param("longitude", "127"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.radiusMeters").value(3000))
                .andExpect(jsonPath("$.dataLoadedAt").isString())
                .andExpect(jsonPath("$.items").isArray());
    }
}
