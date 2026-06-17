package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String module;

    @Column(nullable = false, length = 240)
    private String description;
}
