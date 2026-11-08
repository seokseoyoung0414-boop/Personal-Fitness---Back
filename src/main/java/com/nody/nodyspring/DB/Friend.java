package com.nody.nodyspring.DB;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "friends",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_friend_request",
                        columnNames = {"requester_userid", "receiver_userid"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    // 친구 신청을 보낸 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_userid", nullable = false)
    private User requester;

    // 친구 신청을 받은 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_userid", nullable = false)
    private User receiver;

    // 친구 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendStatus status;

    // 신청 날짜
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수락 날짜
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = FriendStatus.PENDING;
        }
    }
}