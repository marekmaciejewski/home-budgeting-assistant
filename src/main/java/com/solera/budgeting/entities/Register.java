package com.solera.budgeting.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Getter
@Setter
public class Register {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private BigDecimal balance;
    private boolean isActive;
    @OneToMany(mappedBy = "sourceRegister")
    private List<Operation> operationsFrom;
    @OneToMany(mappedBy = "targetRegister")
    private List<Operation> operationsTo;

}
