package pl.mm.homebudget.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import pl.mm.homebudget.api.dto.OperationType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Table("OPERATIONS")
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
    @Column("OPERATION_TYPE")
    private OperationType operationType;
    @Column("SEQUENCE_NUMBER")
    private Long sequenceNumber;
    @Column("PREVIOUS_HASH")
    private String previousHash;
    @Column("PAYLOAD_HASH")
    private String payloadHash;
    @Column("OPERATION_HASH")
    private String operationHash;
    @Column("SOURCE_REGISTER_ID")
    private String sourceRegisterId;
    @Column("TARGET_REGISTER_ID")
    private String targetRegisterId;

}
