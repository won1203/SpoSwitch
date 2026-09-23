package com.sposwitch.backend.exercise.service;

import com.sposwitch.backend.exercise.client.FitnessVideoClient;
import com.sposwitch.backend.exercise.client.FitnessVideoClient.Video;
import java.net.URI;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ExerciseRecommendationService {

    private final FitnessVideoClient client;

    public ExerciseRecommendationService(FitnessVideoClient client) {
        this.client = client;
    }

    public Recommendation recommend(Criteria criteria) {
        validate(criteria);
        List<ExerciseVideo> videos = select(client.fetchVideos(), criteria);
        return new Recommendation(
                videos,
                "국민체력100 · 서울올림픽기념국민체육진흥공단",
                "입력한 기준과 동영상 메타정보를 비교한 운동 안내입니다. 체력 수준에 대한 전문 평가나 의료 처방은 아닙니다."
        );
    }

    static List<ExerciseVideo> select(List<Video> source, Criteria criteria) {
        Map<String, ScoredVideo> unique = new LinkedHashMap<>();
        for (Video video : source) {
            if (!httpUrl(video.videoUrl()) || !ageMatches(video.ageGroup(), criteria.age())
                    || !placeMatches(video, criteria.environment())
                    || !equipmentMatches(video, criteria.equipment(), criteria.environment())
                    || !levelMatches(video, criteria.fitnessLevel())) {
                continue;
            }
            int goalScore = goalScore(video, criteria.goal());
            if (goalScore == 0) continue;
            int score = goalScore + (video.ageGroup().contains(criteria.age()) ? 3 : 0)
                    + (video.place().isBlank() ? 0 : 2)
                    + (video.operation().equals("TODZ_VDO_ROUTINE_I") ? 2 : 0)
                    + (levelText(video).contains(criteria.fitnessLevel()) ? 2 : 0);
            ExerciseVideo result = new ExerciseVideo(
                    video.title().isBlank() ? video.exerciseName() : video.title(),
                    video.exerciseName(), video.description(), video.videoUrl(), video.duration(),
                    video.ageGroup(), video.place(), video.purpose(), video.equipment(), video.fitnessFactor(),
                    video.operation().equals("TODZ_VDO_ROUTINE_I") ? "목적별 루틴" : "개별 운동"
            );
            unique.merge(video.videoUrl(), new ScoredVideo(result, score),
                    (oldValue, newValue) -> oldValue.score() >= newValue.score() ? oldValue : newValue);
        }
        return unique.values().stream()
                .sorted(Comparator.comparingInt(ScoredVideo::score).reversed()
                        .thenComparing(item -> item.video().title()))
                .limit(6)
                .map(ScoredVideo::video)
                .toList();
    }

    private static void validate(Criteria criteria) {
        if (!List.of("10대", "20대", "30대", "40대", "50대", "60대 이상").contains(criteria.age())
                || !List.of("초급", "중급", "고급").contains(criteria.fitnessLevel())
                || !List.of("근력 및 근육 강화", "체지방 감소", "유연성 및 자세 개선", "기초 체력 향상").contains(criteria.goal())
                || !List.of("HOME", "INDOOR_FACILITY", "OUTDOOR").contains(criteria.environment())
                || !List.of("NONE", "MAT", "DUMBBELL", "BAND").contains(criteria.equipment())) {
            throw new IllegalArgumentException("운동 처방 조회 조건이 올바르지 않습니다.");
        }
    }

    private static boolean httpUrl(String url) {
        try {
            URI uri = URI.create(url);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && !uri.getPath().endsWith("/");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean ageMatches(String group, String age) {
        if (group.isBlank() || containsAny(group, "전체", "공통", "전연령")) return true;
        if (group.contains(age) || age.contains(group)) return true;
        if (containsAny(group, "성인", "청장년")) return !age.equals("10대") && !age.equals("60대 이상");
        if (containsAny(group, "노인", "어르신", "고령")) return age.equals("60대 이상");
        if (containsAny(group, "청소년", "학생")) return age.equals("10대");
        // Do not treat an unknown age label as a verified match.
        return false;
    }

    private static boolean placeMatches(Video video, String environment) {
        String place = video.place();
        if (place.isBlank() || containsAny(place, "전체", "공통")) return true;
        return switch (environment) {
            case "HOME" -> !containsAny(place, "야외", "실외", "공원", "체육관", "헬스", "시설")
                    && containsAny(place, "집", "가정", "실내");
            case "INDOOR_FACILITY" -> !containsAny(place, "집", "가정", "야외", "실외", "공원")
                    && containsAny(place, "실내", "체육관", "헬스", "시설");
            default -> containsAny(place, "야외", "실외", "공원");
        };
    }

    private static boolean equipmentMatches(Video video, String equipment, String environment) {
        String tool = video.equipment();
        String text = tool + " " + video.title() + " " + video.exerciseName();
        boolean mat = containsAny(text, "매트", "매트리스");
        boolean dumbbell = containsAny(text, "덤벨", "아령");
        boolean band = containsAny(text, "밴드", "튜빙");
        boolean other = containsAny(text, "짐볼", "물병", "보온병", "냄비", "계단", "케틀벨", "스텝박스", "폼롤러", "품롤러", "의자", "수건");
        if ("INDOOR_FACILITY".equals(environment)) return true;
        if ("OUTDOOR".equals(environment)) {
            return !mat && !dumbbell && !band && !other &&
                    (tool.isBlank() || containsAny(tool, "없음", "맨몸", "무기구", "불필요"));
        }
        if (other) return false;
        if (mat || dumbbell || band) {
            return (mat && "MAT".equals(equipment) && !dumbbell && !band)
                    || (dumbbell && "DUMBBELL".equals(equipment) && !mat && !band)
                    || (band && "BAND".equals(equipment) && !mat && !dumbbell);
        }
        return tool.isBlank() || containsAny(tool, "없음", "맨몸", "무기구", "불필요");
    }

    private static boolean levelMatches(Video video, String level) {
        String text = levelText(video);
        if (level.equals("초급") && containsAny(text, "고급", "고강도", "상급")) return false;
        if (level.equals("고급") && containsAny(text, "초급", "저강도", "입문")) return false;
        if (level.equals("중급") && containsAny(text, "초급", "고급", "입문", "상급")) return false;
        return true;
    }

    private static String levelText(Video video) {
        return video.trainingType() + " " + video.title() + " " + video.description();
    }

    private static int goalScore(Video video, String goal) {
        String terms = switch (goal) {
            case "근력 및 근육 강화" -> "근력,근육,근지구력,저항,웨이트,스쿼트,런지,푸쉬 업,푸쉬업,플랭크";
            case "체지방 감소" -> "체지방,유산소,심폐,다이어트,체중,걷기,달리기,점핑";
            case "유연성 및 자세 개선" -> "유연,스트레칭,자세,평형,균형,가동성,요가,이완";
            default -> "기초,체력,전신,심폐,유산소,걷기";
        };
        String purpose = video.purpose().toLowerCase(Locale.ROOT);
        String title = video.title().toLowerCase(Locale.ROOT);
        String exercise = video.exerciseName().toLowerCase(Locale.ROOT);
        String factor = video.fitnessFactor().toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms.split(",")) {
            if (purpose.contains(term)) score += 8;
            if (title.contains(term)) score += 4;
            if (video.operation().equals("TODZ_VDO_TRNG_VIDEO_I") && exercise.contains(term)) score += 4;
        }
        if (score == 0) return 0;
        for (String term : terms.split(",")) if (factor.contains(term)) score += 1;
        return score;
    }

    private static boolean containsAny(String text, String... terms) {
        for (String term : terms) if (text.contains(term)) return true;
        return false;
    }

    private record ScoredVideo(ExerciseVideo video, int score) {
    }

    public record Criteria(String age, String fitnessLevel, String goal, String environment, String equipment) {
    }

    public record ExerciseVideo(
            String title, String exerciseName, String description, String videoUrl, String duration,
            String ageGroup, String place, String purpose, String equipment, String fitnessFactor, String category
    ) {
    }

    public record Recommendation(List<ExerciseVideo> videos, String source, String note) {
    }
}
