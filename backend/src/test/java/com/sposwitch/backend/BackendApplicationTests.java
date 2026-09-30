package com.sposwitch.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sposwitch.backend.facility.SportsFacilityClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.ClassUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "external-api.sports-facility.service-key=test-key",
        "external-api.kakao.rest-api-key=test-key",
        "external-api.kma.service-key=test-key",
        "external-api.fitness100.service-key=test-key",
        "external-api.airkorea.service-key=test-key"
})
class BackendApplicationTests {

    @Autowired
    ApplicationContext context;

    @LocalServerPort
    int port;

    @MockitoBean
    SportsFacilityClient sportsFacilityClient;

    @Test
    void serverStartsAndHealthIsUpWithoutDatabase() throws Exception {
        assertTrue(context.getBeansOfType(DataSource.class).isEmpty());
        for (String type : new String[] {"com.mysql.cj.jdbc.Driver", "org.hibernate.Session", "org.flywaydb.core.Flyway"}) {
            assertFalse(ClassUtils.isPresent(type, getClass().getClassLoader()), type);
        }
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/actuator/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""), response.body());
    }

}
