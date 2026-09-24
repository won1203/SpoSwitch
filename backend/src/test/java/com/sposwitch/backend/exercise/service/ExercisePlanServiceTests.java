package com.sposwitch.backend.exercise.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sposwitch.backend.exercise.client.FitnessVideoClient.StandardExercise;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExercisePlanServiceTests {

    @Test
    void groupsAdultWeeksAndPhasesWithoutRepeatingFrameRowsOrInventingDosage() {
        List<StandardExercise> rows = List.of(
                row("성인", "1주차", "준비 운동", "손목 돌리기", "", "", ""),
                row("성인", "1주차", "준비 운동", "손목 돌리기", "30초", "1~1", "5~5"),
                row("성인", "1주차", "본 운동", "스쿼트", "", "", ""),
                row("어르신", "1주차", "본 운동", "의자 운동", "", "", ""),
                row("성인", "2주차", "정리 운동", "정리 스트레칭", "", "", "")
        );

        List<ExercisePlanService.WeekPlan> weeks = ExercisePlanService.selectStandard(rows, "20대");

        assertEquals(List.of(1, 2), weeks.stream().map(ExercisePlanService.WeekPlan::week).toList());
        assertEquals(List.of("준비 운동", "본 운동"),
                weeks.getFirst().phases().stream().map(ExercisePlanService.PlanPhase::name).toList());
        assertEquals(1, weeks.getFirst().phases().getFirst().exercises().size());
        assertEquals("30초", weeks.getFirst().phases().getFirst().exercises().getFirst().duration());
        assertEquals("1~1", weeks.getFirst().phases().getFirst().exercises().getFirst().sets());
        assertEquals("", weeks.getFirst().phases().get(1).exercises().getFirst().repetitions());
        assertEquals(1, ExercisePlanService.selectStandard(rows, "60대 이상").size());
        assertEquals(List.of(), ExercisePlanService.selectStandard(rows, "10대"));
    }

    private static StandardExercise row(String age, String week, String phase, String exercise,
                                         String duration, String sets, String repetitions) {
        return new StandardExercise(age + " " + week + " 운동프로그램", age, week, phase, exercise,
                duration, sets, repetitions, "https://example.org/program.mp4");
    }
}
