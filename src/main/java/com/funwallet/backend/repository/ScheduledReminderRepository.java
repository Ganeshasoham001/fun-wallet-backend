package com.funwallet.backend.repository;

import com.funwallet.backend.model.ScheduledReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ScheduledReminderRepository extends JpaRepository<ScheduledReminder, Long> {

    List<ScheduledReminder> findBySentFalseAndScheduledTimeLessThanEqual(Instant now);

    List<ScheduledReminder> findAllByOrderByScheduledTimeDesc();

    List<ScheduledReminder> findByCreatedByOrderByScheduledTimeDesc(String createdBy);
}
