package com.sposwitch.backend.environment.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KmaGridConverterTests {

    private final KmaGridConverter converter = new KmaGridConverter();

    @Test
    void convertsSeoulCoordinatesToKmaGrid() {
        KmaGridConverter.GridPoint point = converter.convert(37.5665, 126.9780);

        assertEquals(60, point.x());
        assertEquals(127, point.y());
    }
}
