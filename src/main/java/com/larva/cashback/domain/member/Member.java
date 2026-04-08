package com.larva.cashback.domain.member;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String name;

    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public Member(String email, String name) {
        this.email = email;
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

}
