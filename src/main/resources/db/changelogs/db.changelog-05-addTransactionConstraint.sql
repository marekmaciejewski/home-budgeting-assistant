--liquibase formatted sql
--changeset Marek:210617-05
--comment registers not null check
ALTER TABLE OPERATION ADD CONSTRAINT OPERATION_CHK CHECK (OPERATION.OPERATIONS_FROM IS NOT NULL
                                                        OR OPERATION.OPERATIONS_TO IS NOT NULL)
--rollback ALTER TABLE OPERATION DROP CONSTRAINT OPERATIONS_CHK
