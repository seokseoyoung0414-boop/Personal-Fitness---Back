package com.nody.nodyspring.DB;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boardby", nullable = false)
    private Board boardby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writeby", nullable = false)
    private User writeby;

    @Column(name = "texts", nullable = false, columnDefinition = "TEXT")
    private String texts;

    @Column(name = "times", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime times;
}
