package cn.edu.shmtu.eduvoyage.assessment.repository;

import cn.edu.shmtu.eduvoyage.assessment.domain.HomeworkQuestion;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Hand-written access to the composite-key {@code homework_question} join table
 * via {@link DatabaseClient}. Holds the per-paper question set with each item's
 * score and order; a paper edit clears and re-inserts the whole set.
 */
@Repository
public class HomeworkQuestionRepository {

    private final DatabaseClient db;

    public HomeworkQuestionRepository(DatabaseClient db) {
        this.db = db;
    }

    public Flux<HomeworkQuestion> findByHomeworkId(Long homeworkId) {
        return db.sql("""
                        SELECT homework_id, question_id, score, sort_no
                        FROM homework_question
                        WHERE homework_id = :hid
                        ORDER BY sort_no, question_id
                        """)
                .bind("hid", homeworkId)
                .map(HomeworkQuestionRepository::map)
                .all();
    }

    public Mono<BigDecimal> sumScore(Long homeworkId) {
        return db.sql("SELECT COALESCE(SUM(score), 0) AS total FROM homework_question WHERE homework_id = :hid")
                .bind("hid", homeworkId)
                .map((row, meta) -> row.get("total", BigDecimal.class))
                .one()
                .defaultIfEmpty(BigDecimal.ZERO);
    }

    public Mono<Void> add(Long homeworkId, Long questionId, BigDecimal score, int sortNo) {
        return db.sql("""
                        INSERT INTO homework_question (homework_id, question_id, score, sort_no)
                        VALUES (:hid, :qid, :score, :sortNo)
                        ON DUPLICATE KEY UPDATE score = :score, sort_no = :sortNo
                        """)
                .bind("hid", homeworkId)
                .bind("qid", questionId)
                .bind("score", score)
                .bind("sortNo", sortNo)
                .then();
    }

    public Mono<Void> deleteByHomeworkId(Long homeworkId) {
        return db.sql("DELETE FROM homework_question WHERE homework_id = :hid")
                .bind("hid", homeworkId)
                .then();
    }

    private static HomeworkQuestion map(Row row, RowMetadata meta) {
        return HomeworkQuestion.builder()
                .homeworkId(row.get("homework_id", Long.class))
                .questionId(row.get("question_id", Long.class))
                .score(row.get("score", BigDecimal.class))
                .sortNo(row.get("sort_no", Integer.class))
                .build();
    }
}
