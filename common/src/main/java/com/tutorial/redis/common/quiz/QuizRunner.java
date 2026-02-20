package com.tutorial.redis.common.quiz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Executes a quiz by evaluating provided answers against the quiz questions.
 */
public class QuizRunner {

    /**
     * Runs a quiz with the given answers and produces a result.
     *
     * @param quiz    the quiz to run
     * @param answers a map of question number to selected option index (zero-based)
     * @return the quiz result
     */
    public static QuizResult run(Quiz quiz, Map<Integer, Integer> answers) {
        int correctCount = 0;
        List<String> incorrectDetails = new ArrayList<>();

        for (QuizQuestion question : quiz.questions()) {
            Integer selectedOption = answers.get(question.number());

            if (selectedOption != null && selectedOption == question.correctOptionIndex()) {
                correctCount++;
            } else {
                String detail = buildIncorrectDetail(question, selectedOption);
                incorrectDetails.add(detail);
            }
        }

        return QuizResult.of(
                quiz.moduleId(),
                quiz.questions().size(),
                correctCount,
                quiz.passingRate(),
                incorrectDetails
        );
    }

    private static String buildIncorrectDetail(QuizQuestion question, Integer selectedOption) {
        StringBuilder sb = new StringBuilder();
        sb.append("Q").append(question.number()).append(": ").append(question.question());

        if (selectedOption == null) {
            sb.append(" | Your answer: (no answer)");
        } else if (selectedOption >= 0 && selectedOption < question.options().size()) {
            sb.append(" | Your answer: ").append(question.options().get(selectedOption));
        } else {
            sb.append(" | Your answer: (invalid option ").append(selectedOption).append(")");
        }

        sb.append(" | Correct answer: ").append(question.options().get(question.correctOptionIndex()));
        sb.append(" | ").append(question.explanation());

        return sb.toString();
    }
}
