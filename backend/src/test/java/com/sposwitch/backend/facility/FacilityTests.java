package com.sposwitch.backend.facility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sposwitch.backend.facility.Facility.ClassificationSource;
import com.sposwitch.backend.facility.Facility.Environment;
import com.sposwitch.backend.facility.Facility.Ownership;
import org.junit.jupiter.api.Test;

class FacilityTests {

    private static SportsFacilityClient.Item item(String status, String sport, String business, String inout,
                                                  String kind, String lat, String lon) {
        return new SportsFacilityClient.Item("ID", "시설", status, sport, business, inout, kind,
                "서울특별시", "양천구", "위탁운영        ", "서울특별시 양천구 신월로 1", null, "02-1234-5678",
                lat, lon, "2025-06-21");
    }

    @Test
    void skipsClosedBilliardsAndUnlocatableFacilities() {
        assertNull(Facility.from(item("폐업", "체력단련장", "체력단련장업", "실내", "신고", "37.52", "126.84")));
        assertNull(Facility.from(item("정상운영", "당구장", "당구장업", "실내", "신고", "37.52", "126.84")));
        assertNull(Facility.from(item("정상운영", "체력단련장", "체력단련장업", "실내", "신고", "", "")));
        assertNull(Facility.from(item("정상운영", "골프", "가상체험 체육시설업", "실내", "신고", "36.49", "127.26")));
    }

    @Test
    void prefersSourceEnvironmentAndInfersFromSportOtherwise() {
        Facility stated = Facility.from(item("정상운영", "테니스장", "테니스장", "실내", "공공", "37.52", "126.84"));
        assertEquals(Environment.INDOOR, stated.environment());
        assertEquals(ClassificationSource.SOURCE, stated.environmentSource());

        Facility court = Facility.from(item("정상운영", "간이운동장", "간이운동장", "없음", "공공", "37.52", "126.84"));
        assertEquals(Environment.OUTDOOR, court.environment());
        assertEquals(ClassificationSource.SPORT_RULE, court.environmentSource());
        assertEquals(Ownership.PUBLIC, court.ownership());
        assertEquals("양천구 · 위탁운영", court.operator());

        Facility gym = Facility.from(item("정상운영", "체력단련장", "체력단련장업", null, "신고", "37.52", "126.84"));
        assertEquals(Environment.INDOOR, gym.environment());
        assertEquals(Ownership.PRIVATE, gym.ownership());
        assertNull(gym.operator());

        Facility unknown = Facility.from(item("정상운영", "기타시설", "기타시설", "", "공공", "37.52", "126.84"));
        assertEquals(Environment.UNKNOWN, unknown.environment());
        assertEquals(ClassificationSource.NONE, unknown.environmentSource());
    }

    @Test
    void namesGolfAndSwimSubtypesByBusinessType() {
        assertEquals("골프연습장", Facility.from(item("정상운영", "스크린", "골프연습장업", "실내", "신고", "37.52", "126.84")).sport());
        assertEquals("수영장", Facility.from(item("정상운영", "실내", "수영장업", "실내", "신고", "37.52", "126.84")).sport());
        assertEquals("태권도", Facility.from(item("정상운영", "태권도", "체육도장업", "실내", "신고", "37.52", "126.84")).sport());
    }

    @Test
    void bothTypeFacilitiesMatchEitherFilter() {
        Facility both = Facility.from(item("정상운영", "수영장", "수영장", "실내외", "공공", "37.52", "126.84"));
        assertTrue(both.matches(Environment.INDOOR));
        assertTrue(both.matches(Environment.OUTDOOR));
        assertTrue(both.matches(null));
    }

    @Test
    void goalKeepsOnlyFittingSports() {
        Facility gym = Facility.from(item("정상운영", "체력단련장", "체력단련장업", "실내", "신고", "37.52", "126.84"));
        Facility taekwondo = Facility.from(item("정상운영", "태권도", "체육도장업", "실내", "신고", "37.52", "126.84"));
        Facility screenGolf = Facility.from(item("정상운영", "스크린", "골프연습장업", "실내", "신고", "37.52", "126.84"));

        assertTrue(Goal.STRENGTH_MUSCLE.matches(gym));
        assertFalse(Goal.STRENGTH_MUSCLE.matches(taekwondo));
        assertTrue(Goal.FLEXIBILITY_POSTURE.matches(taekwondo));
        assertTrue(Goal.GENERAL_FITNESS.matches(gym));

        // Screen golf fits none of the four goals, so it never shows up as a goal match.
        for (Goal goal : Goal.values()) {
            assertFalse(goal.matches(screenGolf), goal.name());
        }
    }

    @Test
    void measuresDistanceInMeters() {
        // 신월4동 → 양천구청 is about 2.7 km.
        double meters = Facility.distanceMeters(37.5230, 126.8370, 37.5170, 126.8665);
        assertTrue(meters > 2_500 && meters < 2_900, "was " + meters);
    }
}
