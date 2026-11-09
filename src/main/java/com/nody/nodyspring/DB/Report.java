package com.nody.nodyspring.DB;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idx;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReportType targetType; // post, comment

    @Column(name = "target_id", nullable = false)
    private Integer targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reporter",
            referencedColumnName = "userid",
            nullable = false
    )
    private User reporter;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "report_date", nullable = false)
    private LocalDateTime reportDate = LocalDateTime.now();
}