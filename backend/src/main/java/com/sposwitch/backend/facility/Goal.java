package com.sposwitch.backend.facility;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maps a user's exercise goal to the facility sports that suit it.
 * ponytail: rule v1 built from the sports actually present in the Seoul dataset.
 * Confirm against 국민체력100 체력요인 values (D-03) before release; a sport in no set simply never matches a goal.
 */
public enum Goal {

    /** 근력 및 근육 강화 — 근력·근지구력 */
    STRENGTH_MUSCLE(Sports.STRENGTH),

    /** 체지방 감소 — 심폐지구력·근지구력 */
    FAT_LOSS(Sports.CARDIO),

    /** 유연성 및 자세 개선 — 유연성·평형성 */
    FLEXIBILITY_POSTURE(Sports.FLEXIBILITY),

    /** 기초 체력 향상 — 심폐지구력·근력 */
    GENERAL_FITNESS(Sports.ALL);

    private final Set<String> sports;

    Goal(Set<String> sports) {
        this.sports = sports;
    }

    public boolean matches(Facility facility) {
        return facility.sport() != null && sports.contains(facility.sport());
    }

    private static final class Sports {

        static final Set<String> STRENGTH = Set.of(
                "체력단련장", "기타체육시설(체력단련장)", "생활체육관", "종합체육시설", "구기체육관",
                "권투", "유도", "레슬링", "복합");

        static final Set<String> CARDIO = Set.of(
                "체력단련장", "기타체육시설(체력단련장)", "생활체육관", "종합체육시설", "구기체육관",
                "수영장", "수영", "줄넘기", "권투", "축구", "축구장", "풋살장", "농구",
                "테니스장", "배드민턴", "롤러스케이트장", "롤러스케이트", "간이운동장", "복합");

        static final Set<String> FLEXIBILITY = Set.of(
                "태권도", "합기도", "검도", "유도", "우슈", "무도학원",
                "실내인공암벽장", "실외인공암벽장", "빙상장", "복합");

        static final Set<String> ALL = Stream.of(STRENGTH, CARDIO, FLEXIBILITY)
                .flatMap(Set::stream)
                .collect(Collectors.toUnmodifiableSet());

        private Sports() {
        }
    }
}
