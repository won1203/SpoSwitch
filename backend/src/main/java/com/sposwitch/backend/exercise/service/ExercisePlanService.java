package com.sposwitch.backend.exercise.service;

import com.sposwitch.backend.common.error.ExternalApiException;
import com.sposwitch.backend.exercise.client.FitnessVideoClient;
import com.sposwitch.backend.exercise.client.FitnessVideoClient.StandardExercise;
import com.sposwitch.backend.exercise.service.ExerciseRecommendationService.Criteria;
import com.sposwitch.backend.exercise.service.ExerciseRecommendationService.ExerciseVideo;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ExercisePlanService {

    private static final List<String> PHASES = List.of("준비 운동", "본 운동", "정리 운동");
    private final FitnessVideoClient client;
    private final ExerciseRecommendationService recommendationService;

    public ExercisePlanService(FitnessVideoClient client, ExerciseRecommendationService recommendationService) {
        this.client = client;
        this.recommendationService = recommendationService;
    }

    public ExercisePlan plan(Criteria criteria) {
        List<ExerciseVideo> matchedVideos = recommendationService.recommend(criteria).videos();
        List<WeekPlan> weeks;
        boolean standardUnavailable = false;
        try {
            weeks = selectStandard(client.fetchStandardExercises(), criteria.age());
        } catch (ExternalApiException exception) {
            weeks = List.of();
            standardUnavailable = true;
        }
        String standardNote = standardUnavailable
                ? "표준운동 프로그램을 현재 불러오지 못했습니다. 목표 운동 영상은 별도로 확인할 수 있습니다."
                : weeks.isEmpty()
                ? "현재 연령대에 해당하는 생애주기별 표준운동 프로그램이 제공되지 않습니다."
                : "표준운동 프로그램은 연령대만 맞춘 참고 자료입니다. 선택한 수준·목표·장소·장비에 맞춘 프로그램은 아닙니다.";
        return new ExercisePlan(
                criteria.age(), criteria.fitnessLevel(), criteria.goal(), criteria.environment(), criteria.equipment(),
                matchedVideos, weeks,
                "국민체력100 동영상 정보 · 서울올림픽기념국민체육진흥공단",
                "현재 입력값으로 고른 운동 영상과 연령대별 표준운동을 함께 보여줍니다. 횟수·시간은 원본 데이터에 있을 때만 표시합니다. 체력측정 결과에 따른 전문 운동처방은 아닙니다.",
                standardNote
        );
    }

    static List<WeekPlan> selectStandard(List<StandardExercise> rows, String age) {
        String expectedAge = age.equals("10대") ? "청소년" : age.equals("60대 이상") ? "어르신" : "성인";
        Map<Integer, MutableWeek> weeks = new LinkedHashMap<>();
        for (StandardExercise row : rows) {
            if (!row.ageGroup().equals(expectedAge) || row.exerciseName().isBlank()
                    || !PHASES.contains(row.phase())) continue;
            int number = weekNumber(row.week());
            if (number == 0) continue;
            MutableWeek week = weeks.computeIfAbsent(number, ignored -> new MutableWeek());
            if (week.title.isBlank()) week.title = row.programTitle();
            if (week.videoUrl.isBlank() && playable(row.videoUrl())) week.videoUrl = row.videoUrl();
            String key = row.phase() + "\u0000" + row.exerciseName();
            PlanExercise current = week.exercises.get(key);
            PlanExercise next = new PlanExercise(row.phase(), row.exerciseName(), row.duration(), row.sets(), row.repetitions());
            week.exercises.put(key, current == null ? next : new PlanExercise(
                    current.phase(), current.name(), prefer(current.duration(), next.duration()),
                    prefer(current.sets(), next.sets()), prefer(current.repetitions(), next.repetitions())
            ));
        }
        List<WeekPlan> result = new ArrayList<>();
        for (int number = 1; number <= 4; number++) {
            MutableWeek week = weeks.get(number);
            if (week == null) continue;
            List<PlanPhase> phases = PHASES.stream().map(phase -> new PlanPhase(phase,
                    week.exercises.values().stream().filter(item -> item.phase().equals(phase)).toList()))
                    .filter(phase -> !phase.exercises().isEmpty()).toList();
            result.add(new WeekPlan(number, week.title.isBlank() ? number + "주차 표준운동" : week.title,
                    week.videoUrl, phases));
        }
        return result;
    }

    private static int weekNumber(String week) {
        for (int number = 1; number <= 4; number++) if (week.contains(number + "주차")) return number;
        return 0;
    }

    private static String prefer(String first, String second) {
        return first.isBlank() ? second : first;
    }

    private static boolean playable(String url) {
        try {
            URI uri = URI.create(url);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static class MutableWeek {
        String title = "";
        String videoUrl = "";
        Map<String, PlanExercise> exercises = new LinkedHashMap<>();
    }

    public record ExercisePlan(
            String age, String fitnessLevel, String goal, String environment, String equipment,
            List<ExerciseVideo> matchedVideos, List<WeekPlan> weeks, String source, String note, String standardNote
    ) {
    }

    public record WeekPlan(int week, String title, String videoUrl, List<PlanPhase> phases) {
    }

    public record PlanPhase(String name, List<PlanExercise> exercises) {
    }

    public record PlanExercise(String phase, String name, String duration, String sets, String repetitions) {
    }
}
