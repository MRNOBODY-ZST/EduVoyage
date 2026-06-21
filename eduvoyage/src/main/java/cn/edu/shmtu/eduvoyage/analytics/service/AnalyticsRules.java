package cn.edu.shmtu.eduvoyage.analytics.service;

import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;

public final class AnalyticsRules {

    private static final int MAX_ACTION_LENGTH = 64;
    private static final int MAX_DURATION_SECONDS = 24 * 60 * 60;

    private AnalyticsRules() {
    }

    public static BizException validateLog(String action, Integer durationSec) {
        String normalized = normalizeAction(action);
        if (normalized.isBlank()) {
            return new BizException(BizErrorCode.LEARNING_LOG_INVALID, "行为类型不能为空");
        }
        if (normalized.length() > MAX_ACTION_LENGTH) {
            return new BizException(BizErrorCode.LEARNING_LOG_INVALID, "行为类型过长");
        }
        if (durationSec != null && (durationSec < 0 || durationSec > MAX_DURATION_SECONDS)) {
            return new BizException(BizErrorCode.LEARNING_LOG_INVALID, "学习时长不合法");
        }
        return null;
    }

    public static String normalizeAction(String action) {
        return action == null ? "" : action.trim().toUpperCase();
    }
}
