package com.tutorial.redis.common.quiz;

import java.util.List;

/**
 * Represents a single question within a quiz.
 *
 * @param number             the question number
 * @param question           the question text
 * @param options            the list of answer options
 * @param correctOptionIndex the zero-based index of the correct option
 * @param explanation        an explanation of the correct answer
 */
public record QuizQuestion(
        int number,
        String question,
        List<String> options,
        int correctOptionIndex,
        String explanation
) {
}
