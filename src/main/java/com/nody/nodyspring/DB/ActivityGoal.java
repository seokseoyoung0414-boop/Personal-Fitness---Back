package com.nody.nodyspring.DB;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "activity_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    // 목표를 생성한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @Column(name = "activity_title", nullable = false, length = 100)
    private String title;

    @Column(name = "activity_comment", nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false, length = 30)
    private String sport;

    // 소수 목표 지원: 예) 러닝 5.5km
    @Column(name = "goal_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal goal;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}