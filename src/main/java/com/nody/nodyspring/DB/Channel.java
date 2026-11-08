package com.nody.nodyspring.DB;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idx;

    // 채널 생성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_userid", nullable = false)
    private User creator;

    // 채널 제목
    @Column(name = "channel_title", nullable = false, length = 100)
    private String channelTitle;

    // 운동 종목
    @Column(nullable = false, length = 50)
    private String sport;

    // 채널 설명
    @Column(name = "channel_comment", nullable = false, columnDefinition = "TEXT")
    private String channelComment;

    // 운동 시작일
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // 운동 종료일
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // 운동 완료 여부
    @Builder.Default
    @Column(nullable = false)
    private Boolean complete = false;

    // 운동 목표
    @Builder.Default
    @Column(nullable = false)
    private int goal = 0;
}