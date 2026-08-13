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
    boolean existsByUserAndChannelAndAuthDate(User user, Channel channel, LocalDate authDate);
    Optional<FitnessAuth> findByUserAndChannelAndAuthDate(User user, Channel channel, LocalDate authDate);

    List<FitnessAuth> findByChannelAndAuthDate(Channel channel, LocalDate authDate);
}