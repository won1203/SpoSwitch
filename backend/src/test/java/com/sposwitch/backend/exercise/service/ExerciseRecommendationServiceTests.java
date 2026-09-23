package com.sposwitch.backend.exercise.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sposwitch.backend.exercise.client.FitnessVideoClient.Video;
import com.sposwitch.backend.exercise.service.ExerciseRecommendationService.Criteria;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ExerciseRecommendationServiceTests {

    @Test
    void matchesGoalAgePlaceEquipmentAndRejectsUnplayableVideos() {
        List<Video> videos = List.of(
                video("덤벨 근력 운동", "https://example.org/dumbbell.mp4", "30대", "가정", "덤벨", "근력 향상"),
                video("스쿼트 근력 운동", "https://example.org/squat.mp4", "30대", "가정", "맨몸", "근력 향상"),
                video("덤벨 근력 운동", "https://example.org/old.mp4", "60대", "가정", "덤벨", "근력 향상"),
                video("야외 근력 운동", "https://example.org/outdoor.mp4", "30대", "야외", "덤벨", "근력 향상"),
                video("유산소 운동", "https://example.org/cardio.mp4", "30대", "가정", "덤벨", "심폐지구력"),
                video("짐볼 근력 운동", "https://example.org/ball.mp4", "30대", "가정", "", "근력 향상"),
                video("품롤러 근력 운동", "https://example.org/roller.mp4", "30대", "가정", "", "근력 향상"),
                video("덤벨 근력 운동", "not-a-url", "30대", "가정", "덤벨", "근력 향상")
        );
        Criteria criteria = new Criteria("30대", "초급", "근력 및 근육 강화", "HOME", "DUMBBELL");

        assertEquals(Set.of("https://example.org/dumbbell.mp4", "https://example.org/squat.mp4"),
                ExerciseRecommendationService.select(videos, criteria).stream()
                        .map(ExerciseRecommendationService.ExerciseVideo::videoUrl).collect(Collectors.toSet()));
    }

    @Test
    void beginnerDoesNotReceiveExplicitHighIntensityContent() {
        Video highIntensity = new Video("고강도 근력 운동", "스쿼트", "고급 루틴",
                "https://example.org/high.mp4", "", "30대", "가정", "근력 향상", "맨몸", "", "고급",
                "TODZ_VDO_ROUTINE_I");
        Video beginner = video("초급 근력 운동", "https://example.org/low.mp4", "30대", "가정", "맨몸", "근력 향상");

        assertEquals(List.of("https://example.org/low.mp4"),
                ExerciseRecommendationService.select(List.of(highIntensity, beginner),
                        new Criteria("30대", "초급", "근력 및 근육 강화", "HOME", "NONE"))
                        .stream().map(ExerciseRecommendationService.ExerciseVideo::videoUrl).toList());
    }

    @Test
    void aFitnessFactorAloneDoesNotTurnDiseasePreventionIntoAStrengthRecommendation() {
        Video prevention = new Video("요통 예방 운동프로그램", "푸쉬 업", "", "https://example.org/back.mp4",
                "", "공통", "", "요통 예방", "", "근력/근지구력", "", "TODZ_VDO_ROUTINE_I");

        assertEquals(List.of(), ExerciseRecommendationService.select(List.of(prevention),
                new Criteria("30대", "초급", "근력 및 근육 강화", "HOME", "NONE")));
    }

    private static Video video(String title, String url, String age, String place, String equipment, String purpose) {
        return new Video(title, title, "", url, "", age, place, purpose, equipment, "", "",
                "TODZ_VDO_ROUTINE_I");
    }
}
