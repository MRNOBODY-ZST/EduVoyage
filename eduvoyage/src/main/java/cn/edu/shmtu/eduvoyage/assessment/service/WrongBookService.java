package cn.edu.shmtu.eduvoyage.assessment.service;

import cn.edu.shmtu.eduvoyage.assessment.domain.Question;
import cn.edu.shmtu.eduvoyage.assessment.domain.WrongBook;
import cn.edu.shmtu.eduvoyage.assessment.dto.WrongBookDtos.WrongBookEntry;
import cn.edu.shmtu.eduvoyage.assessment.repository.WrongBookRepository;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A student's personal wrong book. Misses are recorded automatically during
 * grading: each wrong answer to a question upserts its row (incrementing
 * {@code wrongCount} and re-opening it if previously mastered), while a later
 * correct answer flags the row {@code mastered}. The unique key
 * {@code (student_id, question_id)} keeps one row per question per student.
 */
@Service
public class WrongBookService {

    private final WrongBookRepository wrongBookRepository;
    private final R2dbcEntityTemplate entityTemplate;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public WrongBookService(WrongBookRepository wrongBookRepository,
                            R2dbcEntityTemplate entityTemplate,
                            IdGenerator idGenerator,
                            Clock clock) {
        this.wrongBookRepository = wrongBookRepository;
        this.entityTemplate = entityTemplate;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /** Records (or accumulates) a wrong answer to a question. */
    public Mono<Void> recordWrong(Long studentId, Question question) {
        return wrongBookRepository.findByStudentAndQuestion(studentId, question.getId())
                .flatMap(existing -> {
                    existing.setWrongCount((existing.getWrongCount() == null ? 0 : existing.getWrongCount()) + 1);
                    existing.setLastWrongAt(now());
                    existing.setMastered(0);
                    return wrongBookRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    WrongBook entry = WrongBook.builder()
                            .id(idGenerator.nextId())
                            .studentId(studentId)
                            .questionId(question.getId())
                            .nodeId(question.getNodeId())
                            .wrongCount(1)
                            .lastWrongAt(now())
                            .mastered(0)
                            .build();
                    return entityTemplate.insert(WrongBook.class).using(entry);
                }))
                .then();
    }

    /** Flags an existing wrong-book row mastered when the student answers correctly. */
    public Mono<Void> recordCorrect(Long studentId, Long questionId) {
        return wrongBookRepository.findByStudentAndQuestion(studentId, questionId)
                .flatMap(existing -> {
                    existing.setMastered(1);
                    return wrongBookRepository.save(existing);
                })
                .then();
    }

    public Mono<List<WrongBookEntry>> list(Long studentId, boolean onlyUnmastered) {
        return (onlyUnmastered
                ? wrongBookRepository.findUnmasteredByStudent(studentId)
                : wrongBookRepository.findByStudent(studentId))
                .map(WrongBookEntry::from)
                .collectList();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
