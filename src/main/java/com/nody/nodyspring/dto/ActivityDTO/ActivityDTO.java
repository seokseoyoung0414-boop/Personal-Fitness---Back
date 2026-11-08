package com.nody.nodyspring.dto.ActivityDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ActivityDTO {

    // 목표 생성 / 수정 요청
    public record GoalRequest(
            String channelTitle,
            String channelComment,
            String sport,
            BigDecimal goal,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    // 목표 조회 응답
    // 기존 프론트의 필드명을 최대한 유지
    public record GoalResponse(
            Long idx,
            String channelTitle,
            String channelComment,
            String sport,
            BigDecimal goal,
            String unit,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            boolean channelBuild,
            boolean complete
    ) {
    }

    // 운동 기록 저장 요청
    public record RecordRequest(
            LocalDateTime recordedAt,
            Long durationSeconds,
            BigDecimal distanceKm,
            BigDecimal calories,
            Long steps
    ) {
    }

    // 개별 운동 기록 응답
    public record RecordResponse(
            Long idx,
            LocalDateTime recordedAt,
            Long durationSeconds,
            BigDecimal distanceKm,
            BigDecimal calories,
            Long steps,
            BigDecimal achievementRate
    ) {
    }

    // 날짜별 그래프 응답
    public record ChartPoint(
            LocalDate date,
            BigDecimal distanceKm,
            Long durationSeconds,
            BigDecimal calories,
            Long steps
    ) {
    }

    // 상세 통계 응답
    public record DetailResponse(
            GoalResponse activity,
            BigDecimal totalDistanceKm,
            Long totalDurationSeconds,
            BigDecimal totalCalories,
            Long totalSteps,
            BigDecimal achievedValue,
            BigDecimal achievementRate,
            Integer totalCount,
            Integer successCount,
            Integer failCount,
            List<ChartPoint> chart,
            List<RecordResponse> records
    ) {}
    // 개인 진행 중인 채널별 운동시간 랭킹 응답
    public record ChannelActivityRankingResponse(
            Integer channelIdx,
            String channelTitle,
            String sport,
            Long totalDurationSeconds
    ) {}
}