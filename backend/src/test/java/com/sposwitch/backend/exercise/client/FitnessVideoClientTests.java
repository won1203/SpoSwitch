package com.sposwitch.backend.exercise.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sposwitch.backend.common.config.Fitness100ApiProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class FitnessVideoClientTests {

    @Test
    void springCanConstructTheClientWithItsConfigurationProperties() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(Fitness100ApiProperties.class,
                    () -> new Fitness100ApiProperties("https://example.org/api", "test-key"));
            context.register(FitnessVideoClient.class);
            context.refresh();
            assertTrue(context.getBean(FitnessVideoClient.class) != null);
        }
    }

    @Test
    void readsBothOfficialOperationsAndCachesThemWithoutDoubleEncodingTheKey() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        List<String> requests = new ArrayList<>();
        server.createContext("/B551014/SRVC_TODZ_VDO_PKG/", exchange -> {
            requests.add(exchange.getRequestURI().toString());
            String body = """
                    {"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE"},
                     "body":{"totalCount":"1","items":{"item":{"vdo_ttl_nm":"근력 운동",
                     "trng_nm":"스쿼트","file_url":"https://example.org/video/","file_nm":"squat.mp4",
                     "aggrp_nm":"30대","trng_aim_nm":"근력 향상"}}}}
                    """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            FitnessVideoClient client = new FitnessVideoClient(
                    new Fitness100ApiProperties(
                            "http://127.0.0.1:" + server.getAddress().getPort() + "/B551014/SRVC_TODZ_VDO_PKG",
                            "a%2Bb%3D"
                    ),
                    HttpClient.newHttpClient()
            );
            assertEquals(2, client.fetchVideos().size());
            assertEquals(2, client.fetchVideos().size());
            assertEquals("https://example.org/video/squat.mp4", client.fetchVideos().getFirst().videoUrl());
            assertEquals(2, requests.size());
            assertTrue(requests.get(0).contains("TODZ_VDO_ROUTINE_I"));
            assertTrue(requests.get(1).contains("TODZ_VDO_TRNG_VIDEO_I"));
            assertTrue(requests.get(0).contains("serviceKey=a%2Bb%3D"));
            assertTrue(requests.get(0).contains("resultType=json"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void readsArrayItemsFromThePublicApiShape() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/B551014/SRVC_TODZ_VDO_PKG/", exchange -> {
            byte[] bytes = """
                    {"header":{"resultCode":"0"},"body":{"totalCount":"2","items":{"item":[
                      {"vdo_ttl_nm":"첫 운동","file_url":"https://example.org/first.mp4"},
                      {"vdo_ttl_nm":"둘째 운동","file_url":"https://example.org/second.mp4"}]}}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            FitnessVideoClient client = new FitnessVideoClient(new Fitness100ApiProperties(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/B551014/SRVC_TODZ_VDO_PKG",
                    "test-key"
            ), HttpClient.newHttpClient());
            assertEquals(4, client.fetchVideos().size());
            assertEquals("둘째 운동", client.fetchVideos().get(1).title());
        } finally {
            server.stop(0);
        }
    }
}
