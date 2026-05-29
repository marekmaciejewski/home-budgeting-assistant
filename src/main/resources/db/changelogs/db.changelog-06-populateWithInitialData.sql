--liquibase formatted sql
--changeset Marek:210617-06
--comment Initial data for the demo
INSERT INTO REGISTER VALUES ('Wallet', 1000.00, true);
INSERT INTO REGISTER VALUES ('Savings', 5000.00, true);
INSERT INTO REGISTER VALUES ('Insurance policy', 0.00, true);
INSERT INTO REGISTER VALUES ('Food expenses', 0.00, true);
INSERT INTO REGISTER VALUES ('Idle', 0.00, false);
--rollback TRUNCATE TABLE OPERATION; TRUNCATE TABLE REGISTER
