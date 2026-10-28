package com.nody.nodyspring.repository;

import com.nody.nodyspring.DB.ActivityGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityGoalRepository
        extends JpaRepository<ActivityGoal, Long> {

    List<ActivityGoal> findByUser_UseridOrderByCreatedAtDesc(String userid);

    Optional<ActivityGoal> findByIdxAndUser_Userid(
            Long idx,
            String userid
    );
}