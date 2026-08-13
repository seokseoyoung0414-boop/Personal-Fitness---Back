package com.nody.nodyspring.DB;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "boards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idx;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contents;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer", nullable = false)
    private User writer;

    @Column(nullable = false)
    private int viewcount = 0;

    @Column(name = "regdate", columnDefinition = "DATETIME")
    private LocalDateTime regdate;

    @PrePersist
    public void prePersist() {
        if (this.regdate == null) {
            this.regdate = LocalDateTime.now();
        }
    }
}
