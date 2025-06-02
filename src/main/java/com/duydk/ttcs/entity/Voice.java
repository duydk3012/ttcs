package com.duydk.ttcs.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "voices")
@Data
public class Voice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String gender;

}
