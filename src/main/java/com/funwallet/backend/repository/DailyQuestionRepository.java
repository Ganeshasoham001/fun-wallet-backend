package com.funwallet.backend.repository;

import com.funwallet.backend.model.DailyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyQuestionRepository extends JpaRepository<DailyQuestion, Long> {
    Optional<DailyQuestion> findByQuestionDate(String questionDate);
    List<DailyQuestion> findByRevealedTrueOrderByQuestionDateDesc();
    List<DailyQuestion> findAllByOrderByQuestionDateDesc();
}
