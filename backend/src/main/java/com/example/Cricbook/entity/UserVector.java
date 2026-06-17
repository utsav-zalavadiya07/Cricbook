package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "user_vectors", indexes = {
        @Index(name = "idx_user_vector_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "preferred_city")
    private String preferredCity;

    @Column(name = "preferred_hour")
    private Integer preferredHour;

    @Column(name = "preferred_price_min")
    private BigDecimal preferredPriceMin;

    @Column(name = "preferred_price_max")
    private BigDecimal preferredPriceMax;
}
