package com.sposwitch.backend.exercise.client;

import com.sposwitch.backend.common.config.Fitness100ApiProperties;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class FitnessVideoClient {

    private static final List<String> OPERATIONS = List.of("TODZ_VDO_ROUTINE_I", "TODZ_VDO_TRNG_VIDEO_I");
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 20;
    private static final Duration CACHE_AGE = Duration.ofMinutes(30);

    private final Fitness100ApiProperties properties;
    private final HttpClient httpClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final Map<String, CacheEntry> cache = new HashMap<>();
    private StandardCacheEntry standardCache;

    @Autowired
    public FitnessVideoClient(Fitness100ApiProperties properties) {
        this(properties, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(7)).build());
    }

    FitnessVideoClient(Fitness100ApiProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    public synchronized List<Video> fetchVideos() {
        List<Video> videos = new ArrayList<>();
        for (String operation : OPERATIONS) {
            CacheEntry entry = cache.get(operation);
            if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
                entry = new CacheEntry(List.copyOf(fetchOperation(operation)), Instant.now().plus(CACHE_AGE));
                cache.put(operation, entry);
            }
            videos.addAll(entry.videos());
        }
        return videos;
    }

    public synchronized List<StandardExercise> fetchStandardExercises() {
        String operation = "TODZ_VDO_STD_FTNS_I";
        StandardCacheEntry entry = standardCache;
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            List<StandardExercise> exercises = new ArrayList<>();
            for (int page = 1; page <= MAX_PAGES; page++) {
                JsonNode body = requestPage(operation, page);
                JsonNode item = body.path("items").path("item");
                if (item.isArray()) {
                    for (JsonNode row : item) exercises.add(mapStandardExercise(row));
                } else if (item.isObject()) {
                    exercises.add(mapStandardExercise(item));
                }
                int total = integer(body.path("totalCount"));
                if (total == 0 || page * PAGE_SIZE >= total) {
                    standardCache = new StandardCacheEntry(List.copyOf(exercises), Instant.now().plus(CACHE_AGE));
                    return standardCache.exercises();
                }
            }
            throw new ExternalApiException("국민체력100 표준운동 목록이 조회 한도를 초과했습니다.");
        }
        return entry.exercises();
    }

    private List<Video> fetchOperation(String operation) {
        List<Video> videos = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            JsonNode body = requestPage(operation, page);
            JsonNode item = body.path("items").path("item");
            if (item.isArray()) {
                for (JsonNode row : item) videos.add(mapVideo(row, operation));
            } else if (item.isObject()) {
                videos.add(mapVideo(item, operation));
            }
            int total = integer(body.path("totalCount"));
            if (total == 0 || page * PAGE_SIZE >= total) return videos;
        }
        throw new ExternalApiException("국민체력100 동영상 목록이 조회 한도를 초과했습니다.");
    }

    private JsonNode requestPage(String operation, int page) {
        String key = properties.serviceKey();
        if (key.matches(".*%[0-9a-fA-F]{2}.*")) {
            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
        }
        String url = properties.baseUrl().replaceAll("/+$", "") + "/" + operation + "?"
                + parameter("serviceKey", key) + "&"
                + parameter("pageNo", Integer.toString(page)) + "&"
                + parameter("numOfRows", Integer.toString(PAGE_SIZE)) + "&"
                + parameter("resultType", "json");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new ExternalApiException("국민체력100 API가 HTTP " + response.statusCode() + "을 반환했습니다.");
            }
            JsonNode root = jsonMapper.readTree(response.body());
            if (root.has("response")) root = root.path("response");
            String resultCode = value(root.path("header"), "resultCode");
            if (!List.of("0", "00", "000").contains(resultCode)) {
                throw new ExternalApiException("국민체력100 API 오류: " + resultCode + " "
                        + value(root.path("header"), "resultMsg"));
            }
            if (!root.path("body").isObject()) {
                throw new ExternalApiException("국민체력100 API 응답에 body가 없습니다.");
            }
            return root.path("body");
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException("국민체력100 API 호출이 중단되었습니다.", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new ExternalApiException("국민체력100 API 호출 또는 응답 해석에 실패했습니다.", exception);
        }
    }

    private static String parameter(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static int integer(JsonNode node) {
        try {
            return Integer.parseInt(node.asText());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String value(JsonNode node, String name) {
        return node.path(name).asText("").trim();
    }

    private static Video mapVideo(JsonNode row, String operation) {
        return new Video(
                value(row, "vdo_ttl_nm"), value(row, "trng_nm"), value(row, "vdo_desc"),
                videoUrl(value(row, "file_url"), value(row, "file_nm")),
                value(row, "vdo_len"), value(row, "aggrp_nm"),
                value(row, "trng_plc_nm"), value(row, "trng_aim_nm"), value(row, "tool_nm"),
                value(row, "ftns_fctr_nm"), value(row, "trng_se_nm"), operation
        );
    }

    private static StandardExercise mapStandardExercise(JsonNode row) {
        return new StandardExercise(
                value(row, "vdo_ttl_nm"), value(row, "aggrp_nm"), value(row, "trng_week_nm"),
                value(row, "trng_sqnc_nm"), value(row, "trng_nm"), value(row, "trng_hr_nm"),
                value(row, "set_cnt_nm"), value(row, "rptt_tcnt_nm"),
                videoUrl(value(row, "file_url"), value(row, "file_nm"))
        );
    }

    private static String videoUrl(String directoryOrUrl, String fileName) {
        if (fileName.startsWith("http://") || fileName.startsWith("https://")) return fileName;
        if (fileName.isBlank() || directoryOrUrl.isBlank()) return directoryOrUrl;
        if (directoryOrUrl.endsWith(fileName) || directoryOrUrl.contains("?")) return directoryOrUrl;
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return directoryOrUrl.replaceAll("/+$", "") + "/" + encodedName;
    }

    private record CacheEntry(List<Video> videos, Instant expiresAt) {
    }

    private record StandardCacheEntry(List<StandardExercise> exercises, Instant expiresAt) {
    }

    public record Video(
            String title, String exerciseName, String description, String videoUrl, String duration,
            String ageGroup, String place, String purpose, String equipment, String fitnessFactor,
            String trainingType, String operation
    ) {
    }

    public record StandardExercise(
            String programTitle, String ageGroup, String week, String phase, String exerciseName,
            String duration, String sets, String repetitions, String videoUrl
    ) {
    }
}
