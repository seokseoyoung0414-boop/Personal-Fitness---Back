package com.nody.nodyspring.repository;

import com.nody.nodyspring.DB.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String>{
    //existsByUserid는 User엔티티에 userid라는 필드가 존재해야 작동.
    //boolean existsByUseridAndEmail(String userid, String email);
    //existsByUseridAndEmail User엔티티에 userid라는 필드와 email이라는 필드가 존재해야 작동.

    // userid가 존재하는지
    boolean existsByUserid(String userid);
    // nickname이 존재하는지
    boolean existsByNickname(String nickname);
    // email이 존재하는지
    boolean existsByEmail(String email);
    // phone이 존재하는지
    boolean existsByPhone(String phone);

    Optional<User> findByUserid(String userid);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.point = u.point + :point WHERE u.userid = :userid")
    void addPoint(@Param("userid") String userid, @Param("point") int point);
}
