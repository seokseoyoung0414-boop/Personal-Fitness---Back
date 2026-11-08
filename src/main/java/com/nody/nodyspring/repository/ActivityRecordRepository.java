
package com.nody.nodyspring.repository;

import com.nody.nodyspring.DB.ActivityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityRecordRepository
        extends JpaRepository<ActivityRecord, Long> {

    // 채널별 운동 기록 조회 (오래된 기록부터)
    List<ActivityRecord> findByChannel_IdxOrderByRecordedAtAscIdxAsc(
            Integer channelIdx
    );

    // 채널에 속한 모든 운동 기록 삭제
    void deleteByChannel_Idx(Integer channelIdx);
}
