package com.crafthub.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "military_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilitaryProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(nullable = false)
    private String unitNumber; // Наприклад, "А1234"

    @Column(nullable = false)
    private String edrpou; // Код ЄДРПОУ частини

    private String commanderName; // ПІБ командира

    private String officialAddress; // Адреса ППД
}