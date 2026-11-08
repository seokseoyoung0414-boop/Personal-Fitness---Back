package com.nody.nodyspring.DB;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "activity_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    // 운동 기록이 속한 채널
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_idx", nullable = false)
    private Channel channel;

    // 실제 운동한 날짜와 시간
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    // 운동시간: 초 단위
    @Column(name = "duration_seconds", nullable = false)
    private Long durationSeconds;

    // 운동거리: km 단위
    @Column(name = "distance_km", nullable = false, precision = 12, scale = 3)
    private BigDecimal distanceKm;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal calories;

    @Column(nullable = false)
    private Long steps;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}