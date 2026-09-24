package com.sposwitch.backend.facility;

import java.util.Set;

/**
 * A Seoul sports facility from the national facility dataset.
 * Public and private facilities are both included; fields the source does not provide stay null.
 */
public record Facility(
        String id,
        String name,
        String sport,
        Environment environment,
        ClassificationSource environmentSource,
        Ownership ownership,
        String operator,
        String roadAddress,
        String lotAddress,
        String phone,
        double latitude,
        double longitude,
        String updatedOn,
        Integer distanceMeters
) {

    public enum Environment { INDOOR, OUTDOOR, BOTH, UNKNOWN }

    /** SOURCE: the dataset states it. SPORT_RULE: inferred from the sport (rule v1). NONE: unknown. */
    public enum ClassificationSource { SOURCE, SPORT_RULE, NONE }

    public enum Ownership { PUBLIC, PRIVATE }

    /** Not physical exercise venues; the design doc rules out recommending them. */
    private static final Set<String> EXCLUDED_SPORTS = Set.of("당구장");

    /** Sports whose facilities are overwhelmingly outdoor in the Seoul data where the source states it. */
    private static final Set<String> OUTDOOR_SPORTS = Set.of(
            "간이운동장", "축구장", "야구장", "테니스장", "롤러스케이트장", "파크골프장",
            "전천후게이트볼장", "국궁장", "풋살장", "실외인공암벽장", "골프장", "실외");

    /** The source uses these as golf/swimming sub-types; the business type names the real sport. */
    private static final Set<String> SUBTYPE_LABELS = Set.of("실내", "실외", "스크린", "복합");

    private static final double EARTH_RADIUS_METERS = 6_371_008.8;

    /** Returns null for closed, excluded, or unlocatable facilities. */
    static Facility from(SportsFacilityClient.Item item) {
        if (!"정상운영".equals(trim(item.faci_stat_nm())) || EXCLUDED_SPORTS.contains(trim(item.ftype_nm()))) {
            return null;
        }
        Double latitude = parse(item.faci_lat());
        Double longitude = parse(item.faci_lot());
        // Rough Seoul box: only rejects missing or clearly wrong coordinates, not a district check.
        if (latitude == null || longitude == null
                || latitude < 37.4 || latitude > 37.72 || longitude < 126.7 || longitude > 127.2) {
            return null;
        }
        String sportType = trim(item.ftype_nm());
        Environment environment = switch (trim(item.inout_gbn_nm())) {
            case "실내" -> Environment.INDOOR;
            case "실외" -> Environment.OUTDOOR;
            case "실내외" -> Environment.BOTH;
            default -> null;
        };
        ClassificationSource source = ClassificationSource.SOURCE;
        if (environment == null) {
            if (OUTDOOR_SPORTS.contains(sportType)) {
                environment = Environment.OUTDOOR;
                source = ClassificationSource.SPORT_RULE;
            } else if (sportType.isEmpty() || "기타시설".equals(sportType)) {
                environment = Environment.UNKNOWN;
                source = ClassificationSource.NONE;
            } else {
                environment = Environment.INDOOR;
                source = ClassificationSource.SPORT_RULE;
            }
        }
        Ownership ownership = switch (trim(item.faci_gb_nm())) {
            case "공공" -> Ownership.PUBLIC;
            case "신고", "등록" -> Ownership.PRIVATE;
            default -> null;
        };
        return new Facility(
                item.faci_cd(),
                trim(item.faci_nm()),
                sportLabel(sportType, trim(item.fcob_nm())),
                environment,
                source,
                ownership,
                ownership == Ownership.PUBLIC ? publicOperator(item) : null,
                blankToNull(item.faci_road_addr()),
                blankToNull(item.faci_addr()),
                blankToNull(item.faci_tel_no()),
                latitude,
                longitude,
                blankToNull(item.updt_dt()),
                null
        );
    }

    Facility atDistanceFrom(double originLatitude, double originLongitude) {
        return new Facility(id, name, sport, environment, environmentSource, ownership, operator,
                roadAddress, lotAddress, phone, latitude, longitude, updatedOn,
                (int) Math.round(distanceMeters(originLatitude, originLongitude, latitude, longitude)));
    }

    boolean matches(Environment wanted) {
        return wanted == null || environment == wanted || environment == Environment.BOTH;
    }

    static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
    }

    private static String sportLabel(String sportType, String businessType) {
        if (!sportType.isEmpty() && !SUBTYPE_LABELS.contains(sportType)) {
            return sportType;
        }
        String label = businessType.endsWith("업") ? businessType.substring(0, businessType.length() - 1) : businessType;
        return label.isEmpty() ? null : label;
    }

    private static String publicOperator(SportsFacilityClient.Item item) {
        String agency = blankToNull(item.fmng_cpb_nm()) != null ? trim(item.fmng_cpb_nm()) : trim(item.fmng_cp_nm());
        String management = trim(item.faci_mng_type_cd());
        String joined = String.join(" · ", java.util.stream.Stream.of(agency, management).filter(s -> !s.isEmpty()).toList());
        return joined.isEmpty() ? null : joined;
    }

    private static Double parse(String value) {
        try {
            return value == null || value.isBlank() ? null : Double.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
