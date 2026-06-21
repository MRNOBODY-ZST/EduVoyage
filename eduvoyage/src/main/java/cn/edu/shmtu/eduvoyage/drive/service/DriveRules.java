package cn.edu.shmtu.eduvoyage.drive.service;

import cn.edu.shmtu.eduvoyage.drive.domain.DriveNode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;

import java.nio.file.Paths;
import java.util.Locale;

/**
 * I/O-free validation and normalization rules for drive names and storage keys.
 */
public final class DriveRules {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_MIME_LENGTH = 128;
    private static final String SHA256_REGEX = "^[0-9a-f]{64}$";
    private static final String CODE_REGEX = "^[A-Za-z0-9]{1,16}$";

    private DriveRules() {
    }

    public static String clientFileName(String submitted) {
        if (submitted == null) {
            return "";
        }
        java.nio.file.Path fileName = Paths.get(submitted).getFileName();
        return fileName == null ? "" : fileName.toString();
    }

    public static String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    public static BizException validateNodeName(String name) {
        String value = normalizeName(name);
        if (value.isBlank()) {
            return new BizException(BizErrorCode.PARAM_INVALID, "名称不能为空");
        }
        if (value.length() > MAX_NAME_LENGTH) {
            return new BizException(BizErrorCode.PARAM_INVALID, "名称不能超过255个字符");
        }
        if (".".equals(value) || "..".equals(value)
                || value.contains("/") || value.contains("\\")
                || value.chars().anyMatch(Character::isISOControl)) {
            return new BizException(BizErrorCode.PARAM_INVALID, "名称包含非法字符");
        }
        return null;
    }

    public static String normalizeMime(String mime) {
        String value = mime == null ? "" : mime.trim();
        if (value.isBlank()) {
            return "application/octet-stream";
        }
        if (value.length() > MAX_MIME_LENGTH || value.chars().anyMatch(Character::isISOControl)) {
            return "application/octet-stream";
        }
        return value;
    }

    public static String normalizeSha256(String sha256) {
        return sha256 == null ? "" : sha256.trim().toLowerCase(Locale.ROOT);
    }

    public static BizException validateSha256(String sha256) {
        String value = normalizeSha256(sha256);
        if (!value.matches(SHA256_REGEX)) {
            return new BizException(BizErrorCode.PARAM_INVALID, "sha256 格式不正确");
        }
        return null;
    }

    public static BizException validateExtractCode(String extractCode) {
        if (extractCode == null || extractCode.isBlank()) {
            return null;
        }
        if (!extractCode.matches(CODE_REGEX)) {
            return new BizException(BizErrorCode.PARAM_INVALID, "提取码仅支持1-16位字母或数字");
        }
        return null;
    }

    public static BizException validateSpace(Integer spaceType, Long courseId) {
        int space = normalizeSpaceType(spaceType);
        if (space == DriveNode.SPACE_COURSE && courseId == null) {
            return new BizException(BizErrorCode.PARAM_INVALID, "课程空间必须指定课程");
        }
        if (space == DriveNode.SPACE_PERSONAL && courseId != null) {
            return new BizException(BizErrorCode.PARAM_INVALID, "个人空间不能指定课程");
        }
        return null;
    }

    public static int normalizeSpaceType(Integer spaceType) {
        return spaceType == null ? DriveNode.SPACE_PERSONAL : spaceType;
    }

    public static BizException validateSpaceType(Integer spaceType) {
        int value = normalizeSpaceType(spaceType);
        if (value != DriveNode.SPACE_PERSONAL && value != DriveNode.SPACE_COURSE) {
            return new BizException(BizErrorCode.PARAM_INVALID, "空间类型不正确");
        }
        return null;
    }

    public static String objectKey(String sha256) {
        String value = normalizeSha256(sha256);
        return "drive/" + value.substring(0, 2) + "/" + value;
    }
}
