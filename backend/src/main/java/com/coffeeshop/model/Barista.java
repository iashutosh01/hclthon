package com.coffeeshop.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "baristas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Barista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Builder.Default
    private double currentWorkloadMinutes = 0;

    @Builder.Default
    private double workloadRatio = 1.0;

    @OneToMany(mappedBy = "barista", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Assignment> assignments = new ArrayList<>();

    public boolean isAvailable() {
        return currentWorkloadMinutes <= 0;
    }
}
