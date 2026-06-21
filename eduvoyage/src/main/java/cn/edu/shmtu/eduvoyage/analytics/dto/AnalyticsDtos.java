package cn.edu.shmtu.eduvoyage.analytics.dto;

import cn.edu.shmtu.eduvoyage.analytics.domain.LearningLog;
import cn.edu.shmtu.eduvoyage.analytics.repository.AnalyticsQueryRepository.GradeTrendRow;
import cn.edu.shmtu.eduvoyage.analytics.repository.AnalyticsQueryRepository.HomeworkStatRow;
import cn.edu.shmtu.eduvoyage.analytics.repository.AnalyticsQueryRepository.NodeMasteryRow;
import cn.edu.shmtu.eduvoyage.analytics.repository.AnalyticsQueryRepository.StudentRankingRow;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    @Schema(description = "学习日志上报")
    public record LearningLogRequest(
            @Schema(description = "课程 id")
            @NotNull(message = "课程 id 不能为空")
            Long courseId,

            @Schema(description = "知识点 id，可空")
            Long nodeId,

            @Schema(description = "行为类型，如 VIEW_COURSEWARE / STUDY_NODE / SUBMIT_HOMEWORK")
            @NotBlank(message = "行为类型不能为空")
            @Size(max = 64, message = "行为类型不能超过64个字符")
            String action,

            @Schema(description = "本次学习时长（秒）")
            @PositiveOrZero(message = "学习时长不能为负")
            Integer durationSec
    ) {
    }

    @Schema(description = "学习日志响应")
    public record LearningLogResponse(
            String id,
            Long userId,
            Long courseId,
            Long nodeId,
            String action,
            Integer durationSec,
            Instant ts
    ) {
        public static LearningLogResponse from(LearningLog log) {
            return new LearningLogResponse(log.getId(), log.getUserId(), log.getCourseId(),
                    log.getNodeId(), log.getAction(), log.getDurationSec(), log.getTs());
        }
    }

    @Schema(description = "成绩趋势点")
    public record GradeTrendPoint(
            Long homeworkId,
            String title,
            BigDecimal score,
            LocalDateTime submittedAt
    ) {
        public static GradeTrendPoint from(GradeTrendRow row) {
            return new GradeTrendPoint(row.homeworkId(), row.title(), zero(row.score()), row.submittedAt());
        }
    }

    @Schema(description = "学生个人学情")
    public record StudentDashboardResponse(
            Long studentId,
            long totalDurationSec,
            long activeDays,
            long enrolledCourses,
            long todoHomeworks,
            BigDecimal averageScore,
            BigDecimal masteryPercent,
            List<GradeTrendPoint> gradeTrend,
            List<LearningLogResponse> recentLogs
    ) {
    }

    @Schema(description = "作业统计")
    public record HomeworkStat(
            Long homeworkId,
            String title,
            long submittedCount,
            long totalStudents,
            BigDecimal submissionRate,
            BigDecimal averageScore
    ) {
        public static HomeworkStat from(HomeworkStatRow row, long totalStudents) {
            return new HomeworkStat(row.homeworkId(), row.title(), row.submittedCount(), totalStudents,
                    percent(row.submittedCount(), totalStudents), zero(row.averageScore()));
        }
    }

    @Schema(description = "学生排名")
    public record StudentRanking(
            Long studentId,
            String studentName,
            BigDecimal averageScore,
            long submittedCount
    ) {
        public static StudentRanking from(StudentRankingRow row) {
            return new StudentRanking(row.studentId(), row.studentName(), zero(row.averageScore()),
                    row.submittedCount());
        }
    }

    @Schema(description = "知识点掌握热力项")
    public record NodeMastery(
            Long nodeId,
            String nodeName,
            BigDecimal averageProgress,
            BigDecimal masteryRate
    ) {
        public static NodeMastery from(NodeMasteryRow row) {
            return new NodeMastery(row.nodeId(), row.nodeName(), zero(row.averageProgress()),
                    zero(row.masteryRate()).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
        }
    }

    @Schema(description = "课程教学分析")
    public record CourseAnalyticsResponse(
            Long courseId,
            long enrolledCount,
            long activeLearners,
            long totalDurationSec,
            BigDecimal submissionRate,
            BigDecimal averageScore,
            List<HomeworkStat> homeworkStats,
            List<StudentRanking> studentRankings,
            List<NodeMastery> masteryHeatmap,
            List<NodeMastery> weakNodes
    ) {
    }

    @Schema(description = "平台运营大盘")
    public record AdminDashboardResponse(
            long totalUsers,
            long activeUsers30d,
            long newUsers30d,
            long totalCourses,
            long totalHomeworks,
            long totalSubmissions,
            long storageUsedBytes
    ) {
    }

    private static BigDecimal percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }
}
