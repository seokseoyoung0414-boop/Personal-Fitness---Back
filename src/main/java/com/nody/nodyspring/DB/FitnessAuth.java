package com.nody.nodyspring.DB;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDate;

// FitnessAuth.java
@Entity
@Table(name = "fitness_auth", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"userid", "channel_id", "auth_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FitnessAuth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idx;

    @ManyToOne
    @JoinColumn(name = "userid", referencedColumnName = "userid", nullable = false, foreignKey = @ForeignKey(name = "fk_fitness_user"))
    private User user;

    @ManyToOne
    @JoinColumn(name = "channel_id", referencedColumnName = "idx", nullable = false, foreignKey = @ForeignKey(name = "fk_fitness_channel"))
    private Channel channel;

    private LocalDate authDate;

    private Boolean goalMet;

    @Column(name = "created_at", updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;

    @Column(name = "achieved_value")
    private Double achievedValue;
}

