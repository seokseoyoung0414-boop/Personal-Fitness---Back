package com.nody.nodyspring.repository;

import com.nody.nodyspring.DB.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Integer> {
    List<Channel> findByCompleteFalse();
    List<Channel> findByStartDateAndChannelBuild(LocalDate startDate, boolean build);
    List<Channel> findByEndDateBeforeAndCompleteFalse(LocalDate today);

    @Query("SELECT ch.channelTitle FROM Channel ch WHERE ch.idx = :id")
    String findTitleById(@Param("id") int id);
}