package cn.edu.shmtu.eduvoyage.analytics.service;

import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsRulesTest {

    @Test
    void rejectsInvalidLearningLog() {
        assertThat(AnalyticsRules.validateLog("", 1).getErrorCode())
                .isEqualTo(BizErrorCode.LEARNING_LOG_INVALID);
        assertThat(AnalyticsRules.validateLog("STUDY", -1).getErrorCode())
                .isEqualTo(BizErrorCode.LEARNING_LOG_INVALID);
        assertThat(AnalyticsRules.validateLog("STUDY", 86_401).getErrorCode())
                .isEqualTo(BizErrorCode.LEARNING_LOG_INVALID);
    }

    @Test
    void normalizesAction() {
        assertThat(AnalyticsRules.normalizeAction(" study_node ")).isEqualTo("STUDY_NODE");
    }
}
