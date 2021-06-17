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
    private String id;
    private BigDecimal balance;
    private boolean isActive;
    @OneToMany(mappedBy = "sourceRegister", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Operation> operationsFrom;
    @OneToMany(mappedBy = "targetRegister", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Operation> operationsTo;

}
