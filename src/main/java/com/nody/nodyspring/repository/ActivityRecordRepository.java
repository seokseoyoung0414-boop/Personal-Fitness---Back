package com.nody.nodyspring.repository;

import com.nody.nodyspring.DB.ActivityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityRecordRepository
        extends JpaRepository<ActivityRecord, Long> {

    List<ActivityRecord> findByGoal_IdxOrderByRecordedAtAscIdxAsc(
            Long goalId
    );

    void deleteByGoal_Idx(Long goalId);
}