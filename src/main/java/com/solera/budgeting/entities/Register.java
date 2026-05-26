package com.solera.budgeting.entities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("REGISTER")
@Getter
@Setter
public class Register {

    @Id
    @Column("ID")
    private String id;
    @Column("BALANCE")
    private BigDecimal balance;
    @Column("IS_ACTIVE")
    private boolean isActive;

}
