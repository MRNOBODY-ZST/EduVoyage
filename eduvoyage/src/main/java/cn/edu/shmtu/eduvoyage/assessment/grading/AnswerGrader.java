package cn.edu.shmtu.eduvoyage.assessment.grading;

import cn.edu.shmtu.eduvoyage.assessment.domain.Question;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Set;

/**
 * Pure, side-effect-free auto-grading of a single objective answer against its
 * question's reference answer. No Spring, no I/O — given the question type, the
 * canonical answer and the student's answer, it decides correctness, so the rules
 * are exhaustively unit-testable in isolation.
 *
 * <p>Conventions for {@link Question#getAnswer()} by type:</p>
 * <ul>
 *   <li>{@code SINGLE}/{@code JUDGE}: a single option key, compared case-insensitively
 *       after trimming (e.g. {@code "A"}, {@code "T"}).</li>
 *   <li>{@code MULTIPLE}: comma-separated option keys; the student's selected set
 *       must match exactly (order-insensitive), so partial selections score zero.</li>
 *   <li>{@code FILL}: expected text, compared after trimming; case-insensitive.</li>
 * </ul>
 * Subjective ({@code SHORT}) answers are not gradable here and always return
 * {@link Verdict#PENDING}.
 */
public final class AnswerGrader {

    private AnswerGrader() {
    }

    /** Outcome of grading one answer. */
    public enum Verdict {
        CORRECT,
        WRONG,
        /** Subjective item awaiting manual review. */
        PENDING
    }

    /**
     * Grades {@code studentAnswer} for a question of {@code type} whose reference
     * answer is {@code referenceAnswer}. A blank student answer to an objective
     * question is {@link Verdict#WRONG} (unanswered), never correct.
     */
    public static Verdict grade(int type, String referenceAnswer, String studentAnswer) {
        if (!Question.isObjective(type)) {
            return Verdict.PENDING;
        }
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return Verdict.WRONG;
        }
        String ref = referenceAnswer == null ? "" : referenceAnswer.trim();
        String ans = studentAnswer.trim();

        return switch (type) {
            case Question.TYPE_MULTIPLE -> keySet(ref).equals(keySet(ans)) && !keySet(ans).isEmpty()
                    ? Verdict.CORRECT : Verdict.WRONG;
            case Question.TYPE_SINGLE, Question.TYPE_JUDGE -> ref.equalsIgnoreCase(ans)
                    ? Verdict.CORRECT : Verdict.WRONG;
            case Question.TYPE_FILL -> ref.equalsIgnoreCase(ans)
                    ? Verdict.CORRECT : Verdict.WRONG;
            default -> Verdict.PENDING;
        };
    }

    /** Normalizes a comma-separated option-key string to an upper-cased, trimmed set. */
    private static Set<String> keySet(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase())
                .collect(Collectors.toSet());
    }
}
