package com.solera.budgeting.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private long id;
    private Instant timestamp;
    private BigDecimal amount;
    @ManyToOne
    @JoinColumn(name = "operationsFrom")
    private Register sourceRegister;
    @ManyToOne
    @JoinColumn(name = "operationsTo")
    private Register targetRegister;

}
