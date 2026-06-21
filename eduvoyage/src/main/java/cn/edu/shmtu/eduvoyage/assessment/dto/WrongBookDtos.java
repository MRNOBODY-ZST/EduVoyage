package cn.edu.shmtu.eduvoyage.assessment.dto;

import cn.edu.shmtu.eduvoyage.assessment.domain.WrongBook;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Wrong-book outbound view. Records are accumulated automatically as a student
 * misses questions and flagged mastered once answered correctly again.
 */
public final class WrongBookDtos {

    private WrongBookDtos() {
    }

    @Schema(description = "错题本条目")
    public record WrongBookEntry(
            @Schema(description = "记录 id") Long id,
            @Schema(description = "题目 id") Long questionId,
            @Schema(description = "知识点 id") Long nodeId,
            @Schema(description = "错误次数") Integer wrongCount,
            @Schema(description = "最近错误时间") LocalDateTime lastWrongAt,
            @Schema(description = "是否已掌握") boolean mastered
    ) {
        public static WrongBookEntry from(WrongBook w) {
            return new WrongBookEntry(w.getId(), w.getQuestionId(), w.getNodeId(),
                    w.getWrongCount(), w.getLastWrongAt(),
                    w.getMastered() != null && w.getMastered() == 1);
        }
    }
}
