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

    @Column(name = "channel_title", nullable = false, length = 100)
    private String channelTitle;

    @Column(nullable = false, length = 50)
    private String sport;

    @Column(name = "channel_comment", nullable = false, columnDefinition = "TEXT")
    private String channelComment;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 50)
    private String modes;

    @Column(nullable = false)
    private Boolean complete = false;

    @Column(name = "channel_build", nullable = false)
    private Boolean channelBuild = false;

    @Column(name = "countuser", nullable = false)
    private int countUser;

    @Column(nullable = false)
    private int goal = 0;
}
