package com.joao.desafio.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long associateId;

    private Boolean vote;

    @ManyToOne
    @JoinColumn(name = "id_agenda")
    private Agenda agenda;
}
