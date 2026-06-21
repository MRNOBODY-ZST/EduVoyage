package cn.edu.shmtu.eduvoyage.interaction.service;

import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InteractionRulesTest {

    @Test
    void rejectsBlankTitleAndContent() {
        assertThat(InteractionRules.validateTitle("  ").getErrorCode())
                .isEqualTo(BizErrorCode.PARAM_INVALID);
        assertThat(InteractionRules.validateContent("\n", 100).getErrorCode())
                .isEqualTo(BizErrorCode.PARAM_INVALID);
    }

    @Test
    void normalizesText() {
        assertThat(InteractionRules.normalize("  hello  ")).isEqualTo("hello");
    }
}
