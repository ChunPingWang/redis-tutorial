package com.tutorial.redis.common.quiz;

import java.util.List;

/**
 * Represents a quiz for a tutorial module.
 *
 * @param title       the title of the quiz
 * @param moduleId    the identifier of the module this quiz belongs to
 * @param questions   the list of quiz questions
 * @param passingRate the minimum score required to pass (0.0 to 1.0)
 */
public record Quiz(
        String title,
        String moduleId,
        List<QuizQuestion> questions,
        double passingRate
) {

    /**
     * Creates a Quiz with the default passing rate of 0.8 (80%).
     *
     * @param title    the title of the quiz
     * @param moduleId the identifier of the module
     * @param questions the list of quiz questions
     */
    public Quiz(String title, String moduleId, List<QuizQuestion> questions) {
        this(title, moduleId, questions, 0.8);
    }
}
