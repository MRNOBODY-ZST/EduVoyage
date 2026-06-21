package cn.edu.shmtu.eduvoyage.assessment.grading;

import cn.edu.shmtu.eduvoyage.assessment.domain.Question;
import cn.edu.shmtu.eduvoyage.assessment.grading.AnswerGrader.Verdict;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the pure {@link AnswerGrader}. Covers each objective type's
 * matching rule, the order/case-insensitivity of multiple choice, blank answers,
 * and the pending verdict for subjective questions.
 */
class AnswerGraderTest {

    @Test
    void singleChoiceMatchesCaseInsensitively() {
        assertThat(AnswerGrader.grade(Question.TYPE_SINGLE, "A", "a")).isEqualTo(Verdict.CORRECT);
        assertThat(AnswerGrader.grade(Question.TYPE_SINGLE, "A", "B")).isEqualTo(Verdict.WRONG);
    }

    @Test
    void judgeMatchesReference() {
        assertThat(AnswerGrader.grade(Question.TYPE_JUDGE, "T", "T")).isEqualTo(Verdict.CORRECT);
        assertThat(AnswerGrader.grade(Question.TYPE_JUDGE, "T", "F")).isEqualTo(Verdict.WRONG);
    }

    @Test
    void multipleChoiceRequiresExactSetRegardlessOfOrder() {
        assertThat(AnswerGrader.grade(Question.TYPE_MULTIPLE, "A,C", "C,A")).isEqualTo(Verdict.CORRECT);
        assertThat(AnswerGrader.grade(Question.TYPE_MULTIPLE, "A,C", "a, c")).isEqualTo(Verdict.CORRECT);
        // partial selection scores zero
        assertThat(AnswerGrader.grade(Question.TYPE_MULTIPLE, "A,C", "A")).isEqualTo(Verdict.WRONG);
        // extra selection scores zero
        assertThat(AnswerGrader.grade(Question.TYPE_MULTIPLE, "A,C", "A,B,C")).isEqualTo(Verdict.WRONG);
    }

    @Test
    void fillTrimsAndIgnoresCase() {
        assertThat(AnswerGrader.grade(Question.TYPE_FILL, "Reactor", "  reactor ")).isEqualTo(Verdict.CORRECT);
        assertThat(AnswerGrader.grade(Question.TYPE_FILL, "Reactor", "Netty")).isEqualTo(Verdict.WRONG);
    }

    @Test
    void blankAnswerToObjectiveIsWrong() {
        assertThat(AnswerGrader.grade(Question.TYPE_SINGLE, "A", "")).isEqualTo(Verdict.WRONG);
        assertThat(AnswerGrader.grade(Question.TYPE_SINGLE, "A", null)).isEqualTo(Verdict.WRONG);
        assertThat(AnswerGrader.grade(Question.TYPE_MULTIPLE, "A,B", "  ")).isEqualTo(Verdict.WRONG);
    }

    @Test
    void shortAnswerIsAlwaysPending() {
        assertThat(AnswerGrader.grade(Question.TYPE_SHORT, "anything", "anything"))
                .isEqualTo(Verdict.PENDING);
        assertThat(AnswerGrader.grade(Question.TYPE_SHORT, null, null)).isEqualTo(Verdict.PENDING);
    }

    @Test
    void emptyReferenceMultipleNeverMatchesEmptyAnswer() {
        // a blank answer is short-circuited to WRONG before set comparison
        assertThat(AnswerGrader.grade(Question.TYPE_MULTIPLE, "", "")).isEqualTo(Verdict.WRONG);
    }
}
