package com.nody.nodyspring.repository;

import com.nody.nodyspring.DB.Channel;
import com.nody.nodyspring.DB.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Integer> {

    // 로그인한 사용자가 생성한 채널 조회
    List<Channel> findByCreator(User creator);

    // 종료일이 지난 미완료 채널 조회
    List<Channel> findByEndDateBeforeAndCompleteFalse(LocalDate today);

    List<Channel> findByCreator_UseridOrderByIdxDesc(String userid);

    // 채널 제목 조회
    @Query("SELECT ch.channelTitle FROM Channel ch WHERE ch.idx = :id")
    String findTitleById(@Param("id") int id);

    // 채널 번호와 생성자 ID로 본인 채널 조회
    Optional<Channel> findByIdxAndCreator_Userid(
            Integer idx,
            String userid
    );
}