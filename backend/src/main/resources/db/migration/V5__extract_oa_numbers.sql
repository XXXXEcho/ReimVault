CREATE TABLE oa_numbers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  number VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO oa_numbers (number) SELECT DISTINCT oa_number FROM reimbursement_records WHERE oa_number IS NOT NULL AND oa_number <> '';

ALTER TABLE reimbursement_records ADD COLUMN oa_id BIGINT NULL;

UPDATE reimbursement_records SET oa_id = (SELECT o.id FROM oa_numbers o WHERE o.number = reimbursement_records.oa_number) WHERE oa_number IS NOT NULL AND oa_number <> '';

ALTER TABLE reimbursement_records ADD CONSTRAINT fk_records_oa FOREIGN KEY (oa_id) REFERENCES oa_numbers(id);

ALTER TABLE reimbursement_records DROP COLUMN oa_number;
