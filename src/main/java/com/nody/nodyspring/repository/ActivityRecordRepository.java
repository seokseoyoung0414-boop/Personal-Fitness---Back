
package com.nody.nodyspring.repository;

import com.nody.nodyspring.DB.ActivityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ActivityRecordRepository
        extends JpaRepository<ActivityRecord, Long> {

    // 채널별 운동 기록 조회 (오래된 기록부터)
    List<ActivityRecord> findByChannel_IdxOrderByRecordedAtAscIdxAsc(
            Integer channelIdx
    );

    // 채널에 속한 모든 운동 기록 삭제
    void deleteByChannel_Idx(Integer channelIdx);

    // 개인 진행 중인 채널별 누적 운동시간 랭킹
    @Query("""
        SELECT
            c.idx AS channelIdx,
            c.channelTitle AS channelTitle,
            c.sport AS sport,
            COALESCE(SUM(ar.durationSeconds), 0) AS totalDurationSeconds
        FROM Channel c
        LEFT JOIN ActivityRecord ar ON ar.channel.idx = c.idx
        WHERE c.creator.userid = :userId
          AND c.startDate <= :today
          AND c.endDate >= :today
        GROUP BY c.idx, c.channelTitle, c.sport
        ORDER BY SUM(ar.durationSeconds) DESC
    """)
    List<ChannelActivityRankingProjection> findMyOngoingChannelRanking(
            @Param("userId") String userId,
            @Param("today") LocalDate today
    );

    // 랭킹 조회용 Projection
    interface ChannelActivityRankingProjection {
        Integer getChannelIdx();
        String getChannelTitle();
        String getSport();
        Long getTotalDurationSeconds();
    }
}
