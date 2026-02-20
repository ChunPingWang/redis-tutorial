package com.tutorial.redis.common.quiz;

import java.util.List;

/**
 * Represents the result of a completed quiz attempt.
 *
 * @param moduleId         the identifier of the module
 * @param totalQuestions   the total number of questions in the quiz
 * @param correctAnswers   the number of correctly answered questions
 * @param score            the score as a ratio (0.0 to 1.0)
 * @param passed           whether the quiz was passed
 * @param incorrectDetails details about incorrectly answered questions
 */
public record QuizResult(
        String moduleId,
        int totalQuestions,
        int correctAnswers,
        double score,
        boolean passed,
        List<String> incorrectDetails
) {

    /**
     * Factory method to create a QuizResult with computed score and pass/fail status.
     *
     * @param moduleId         the module identifier
     * @param totalQuestions   the total number of questions
     * @param correctAnswers   the number of correct answers
     * @param passingRate      the minimum score required to pass
     * @param incorrectDetails details about incorrect answers
     * @return a new QuizResult instance
     */
    public static QuizResult of(String moduleId, int totalQuestions, int correctAnswers,
                                 double passingRate, List<String> incorrectDetails) {
        double computedScore = totalQuestions > 0 ? (double) correctAnswers / totalQuestions : 0.0;
        boolean computedPassed = computedScore >= passingRate;
        return new QuizResult(moduleId, totalQuestions, correctAnswers,
                computedScore, computedPassed, incorrectDetails);
    }
}
