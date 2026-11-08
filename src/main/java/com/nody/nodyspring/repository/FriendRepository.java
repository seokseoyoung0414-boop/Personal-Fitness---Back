package com.nody.nodyspring.repository;

import com.nody.nodyspring.DB.Friend;
import com.nody.nodyspring.DB.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    // 같은 방향의 친구 신청 확인
    Optional<Friend> findByRequester_UseridAndReceiver_UseridAndStatus(
            String requesterUserid,
            String receiverUserid,
            FriendStatus status
    );

    // 신청이 반대 방향으로 존재하는지 확인
    Optional<Friend> findByRequester_UseridAndReceiver_Userid(
            String requesterUserid,
            String receiverUserid
    );

    // 받은 친구 신청
    List<Friend> findByReceiver_UseridAndStatusOrderByCreatedAtDesc(
            String userid,
            FriendStatus status
    );

    // 보낸 친구 신청
    List<Friend> findByRequester_UseridAndStatusOrderByCreatedAtDesc(
            String userid,
            FriendStatus status
    );

    // 특정 사용자의 친구 관계 조회
    @Query("""
        SELECT f
        FROM Friend f
        WHERE f.status = :status
          AND (
              f.requester.userid = :userid
              OR f.receiver.userid = :userid
          )
        ORDER BY f.acceptedAt DESC
    """)
    List<Friend> findAllByUserAndStatus(
            @Param("userid") String userid,
            @Param("status") FriendStatus status
    );

    // 두 사용자가 친구인지 확인
    @Query("""
        SELECT COUNT(f) > 0
        FROM Friend f
        WHERE f.status = :status
          AND (
              (f.requester.userid = :userA
               AND f.receiver.userid = :userB)
              OR
              (f.requester.userid = :userB
               AND f.receiver.userid = :userA)
          )
    """)
    boolean existsFriendRelation(
            @Param("userA") String userA,
            @Param("userB") String userB,
            @Param("status") FriendStatus status
    );
}