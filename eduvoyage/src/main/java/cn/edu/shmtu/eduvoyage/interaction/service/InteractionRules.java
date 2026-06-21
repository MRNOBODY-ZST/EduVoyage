package cn.edu.shmtu.eduvoyage.interaction.service;

import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;

/**
 * I/O-free interaction validations.
 */
public final class InteractionRules {

    private InteractionRules() {
    }

    public static BizException validateTitle(String title) {
        String value = normalize(title);
        if (value.isBlank()) {
            return new BizException(BizErrorCode.PARAM_INVALID, "标题不能为空");
        }
        if (value.length() > 200) {
            return new BizException(BizErrorCode.PARAM_INVALID, "标题不能超过200个字符");
        }
        return null;
    }

    public static BizException validateContent(String content, int maxLength) {
        String value = normalize(content);
        if (value.isBlank()) {
            return new BizException(BizErrorCode.PARAM_INVALID, "内容不能为空");
        }
        if (value.length() > maxLength) {
            return new BizException(BizErrorCode.PARAM_INVALID, "内容过长");
        }
        return null;
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
