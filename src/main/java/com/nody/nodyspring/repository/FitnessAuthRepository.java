
package com.nody.nodyspring.repository;

import com.nody.nodyspring.DB.FitnessAuth;
import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.DB.Channel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FitnessAuthRepository extends JpaRepository<FitnessAuth, Integer> {

    // 특정 사용자의 특정 채널에 오늘 인증 기록이 있는지 확인
    boolean existsByUserAndChannelAndAuthDate(
            User user,
            Channel channel,
            LocalDate authDate
    );

    // 특정 사용자의 특정 채널에 해당 날짜 인증 기록 조회
    Optional<FitnessAuth> findByUserAndChannelAndAuthDate(
            User user,
            Channel channel,
            LocalDate authDate
    );

    // 특정 채널의 특정 날짜 인증 기록 조회
    List<FitnessAuth> findByChannelAndAuthDate(
            Channel channel,
            LocalDate authDate
    );

    // 특정 채널에 연결된 모든 인증 기록 삭제
    void deleteByChannel(Channel channel);
}
