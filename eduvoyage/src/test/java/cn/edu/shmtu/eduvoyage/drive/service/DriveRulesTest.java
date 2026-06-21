package cn.edu.shmtu.eduvoyage.drive.service;

import cn.edu.shmtu.eduvoyage.drive.domain.DriveNode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DriveRulesTest {

    @Test
    void rejectsTraversalNames() {
        BizException slash = DriveRules.validateNodeName("../secret.txt");
        BizException backslash = DriveRules.validateNodeName("..\\secret.txt");
        BizException dotdot = DriveRules.validateNodeName("..");

        assertThat(slash.getErrorCode()).isEqualTo(BizErrorCode.PARAM_INVALID);
        assertThat(backslash.getErrorCode()).isEqualTo(BizErrorCode.PARAM_INVALID);
        assertThat(dotdot.getErrorCode()).isEqualTo(BizErrorCode.PARAM_INVALID);
    }

    @Test
    void normalizesShaAndBuildsServerObjectKey() {
        String sha = "ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789";

        assertThat(DriveRules.normalizeSha256(sha))
                .isEqualTo("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789");
        assertThat(DriveRules.validateSha256(sha)).isNull();
        assertThat(DriveRules.objectKey(sha))
                .isEqualTo("drive/ab/abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789");
    }

    @Test
    void validatesSpaceContract() {
        assertThat(DriveRules.validateSpaceType(9).getErrorCode()).isEqualTo(BizErrorCode.PARAM_INVALID);
        assertThat(DriveRules.validateSpace(DriveNode.SPACE_COURSE, null).getErrorCode())
                .isEqualTo(BizErrorCode.PARAM_INVALID);
        assertThat(DriveRules.validateSpace(DriveNode.SPACE_PERSONAL, 1L).getErrorCode())
                .isEqualTo(BizErrorCode.PARAM_INVALID);
        assertThat(DriveRules.validateSpace(DriveNode.SPACE_PERSONAL, null)).isNull();
    }

    @Test
    void validatesExtractCode() {
        assertThat(DriveRules.validateExtractCode("A1b2")).isNull();
        assertThat(DriveRules.validateExtractCode("bad-code").getErrorCode())
                .isEqualTo(BizErrorCode.PARAM_INVALID);
    }
}
