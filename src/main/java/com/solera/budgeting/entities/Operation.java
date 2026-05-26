package com.solera.budgeting.entities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Table("OPERATION")
@Getter
@Setter
public class Operation {

    @Id
    @Column("ID")
    private Long id;
    @Column("TIMESTAMP")
    private Instant timestamp;
    @Column("AMOUNT")
    private BigDecimal amount;
    @Column("OPERATIONS_FROM")
    private String sourceRegisterId;
    @Column("OPERATIONS_TO")
    private String targetRegisterId;

}
