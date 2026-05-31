ALTER TABLE reimbursement_records ADD COLUMN batch_id BIGINT NULL;
ALTER TABLE reimbursement_records ADD CONSTRAINT fk_records_batch FOREIGN KEY (batch_id) REFERENCES reimbursement_batches(id);

UPDATE reimbursement_records SET batch_id = (SELECT bi.batch_id FROM reimbursement_batch_items bi WHERE bi.record_id = reimbursement_records.id) WHERE EXISTS (SELECT 1 FROM reimbursement_batch_items bi WHERE bi.record_id = reimbursement_records.id);
