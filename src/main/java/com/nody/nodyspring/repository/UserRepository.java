package com.nody.nodyspring.repository;

import com.nody.nodyspring.DB.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // userid 존재 여부
    boolean existsByUserid(String userid);

    // nickname 존재 여부
    boolean existsByNickname(String nickname);

    // email 존재 여부
    boolean existsByEmail(String email);

    // phone 존재 여부
    boolean existsByPhone(String phone);

    // userid로 사용자 조회
    Optional<User> findByUserid(String userid);

    // phone으로 사용자 조회
    Optional<User> findByPhone(String phone);

    // email로 사용자 조회
    Optional<User> findByEmail(String email);
}