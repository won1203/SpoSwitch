package com.sposwitch.backend.environment.weather;

import org.springframework.stereotype.Component;

@Component
public class KmaGridConverter {

    private static final double EARTH_RADIUS_KM = 6371.00877;
    private static final double GRID_KM = 5.0;
    private static final double STANDARD_LATITUDE_1 = 30.0;
    private static final double STANDARD_LATITUDE_2 = 60.0;
    private static final double ORIGIN_LONGITUDE = 126.0;
    private static final double ORIGIN_LATITUDE = 38.0;
    private static final double ORIGIN_X = 43.0;
    private static final double ORIGIN_Y = 136.0;

    public GridPoint convert(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("위도는 -90에서 90 사이여야 합니다.");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("경도는 -180에서 180 사이여야 합니다.");
        }

        double degreesToRadians = Math.PI / 180.0;
        double re = EARTH_RADIUS_KM / GRID_KM;
        double slat1 = STANDARD_LATITUDE_1 * degreesToRadians;
        double slat2 = STANDARD_LATITUDE_2 * degreesToRadians;
        double olon = ORIGIN_LONGITUDE * degreesToRadians;
        double olat = ORIGIN_LATITUDE * degreesToRadians;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5)
                / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + latitude * degreesToRadians * 0.5);
        ra = re * sf / Math.pow(ra, sn);

        double theta = longitude * degreesToRadians - olon;
        if (theta > Math.PI) {
            theta -= 2.0 * Math.PI;
        }
        if (theta < -Math.PI) {
            theta += 2.0 * Math.PI;
        }
        theta *= sn;

        int x = (int) Math.floor(ra * Math.sin(theta) + ORIGIN_X + 0.5);
        int y = (int) Math.floor(ro - ra * Math.cos(theta) + ORIGIN_Y + 0.5);
        return new GridPoint(x, y);
    }

    public record GridPoint(int x, int y) {
    }
}
