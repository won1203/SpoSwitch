package com.sposwitch.backend.exercise.controller;

import com.sposwitch.backend.exercise.service.ExerciseRecommendationService;
import com.sposwitch.backend.exercise.service.ExerciseRecommendationService.Criteria;
import com.sposwitch.backend.exercise.service.ExerciseRecommendationService.Recommendation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exercises")
public class ExerciseController {

    private final ExerciseRecommendationService service;

    public ExerciseController(ExerciseRecommendationService service) {
        this.service = service;
    }

    @GetMapping("/recommendations")
    public Recommendation recommendations(
            @RequestParam String age,
            @RequestParam String fitnessLevel,
            @RequestParam String goal,
            @RequestParam String environment,
            @RequestParam String equipment
    ) {
        return service.recommend(new Criteria(age, fitnessLevel, goal, environment, equipment));
    }
}
