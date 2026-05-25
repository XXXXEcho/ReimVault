ALTER TABLE reimbursement_records ADD COLUMN batch_id BIGINT NULL;
ALTER TABLE reimbursement_records ADD CONSTRAINT fk_records_batch FOREIGN KEY (batch_id) REFERENCES reimbursement_batches(id);

UPDATE reimbursement_records r
  JOIN reimbursement_batch_items bi ON r.id = bi.record_id
  SET r.batch_id = bi.batch_id;
