package com.sposwitch.backend.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sposwitch.backend.environment.controller.WeatherController;
import com.sposwitch.backend.environment.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WeatherController.class)
class ApiExceptionHandlerTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    WeatherService weatherService;

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
}
