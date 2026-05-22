create table users (
  id bigint not null auto_increment primary key,
  username varchar(80) not null unique,
  display_name varchar(80) not null,
  department varchar(120),
  password_hash varchar(120) not null,
  role varchar(20) not null,
  enabled boolean not null default true,
  created_at timestamp not null,
  updated_at timestamp not null
);

create table expense_categories (
  id bigint not null auto_increment primary key,
  name varchar(80) not null unique,
  enabled boolean not null default true,
  sort_order int not null default 0,
  remark varchar(255),
  created_at timestamp not null,
  updated_at timestamp not null
);

create table reimbursement_records (
  id bigint not null auto_increment primary key,
  employee_id bigint not null,
  amount decimal(12,2) not null,
  category_id bigint not null,
  purpose varchar(255) not null,
  payment_time timestamp not null,
  status varchar(20) not null,
  admin_remark varchar(500),
  created_at timestamp not null,
  updated_at timestamp not null,
  submitted_at timestamp,
  archived_at timestamp,
  constraint fk_records_employee foreign key (employee_id) references users(id),
  constraint fk_records_category foreign key (category_id) references expense_categories(id)
);

create table reimbursement_attachments (
  id bigint not null auto_increment primary key,
  record_id bigint not null,
  type varchar(40) not null,
  original_filename varchar(255) not null,
  storage_path varchar(500) not null,
  content_type varchar(120) not null,
  size_bytes bigint not null,
  created_at timestamp not null,
  constraint fk_attachments_record foreign key (record_id) references reimbursement_records(id)
);

create table reimbursement_batches (
  id bigint not null auto_increment primary key,
  name varchar(120) not null unique,
  description varchar(500),
  created_by bigint not null,
  created_at timestamp not null,
  archived_at timestamp,
  constraint fk_batches_creator foreign key (created_by) references users(id)
);

create table reimbursement_batch_items (
  id bigint not null auto_increment primary key,
  batch_id bigint not null,
  record_id bigint not null unique,
  created_at timestamp not null,
  constraint fk_batch_items_batch foreign key (batch_id) references reimbursement_batches(id),
  constraint fk_batch_items_record foreign key (record_id) references reimbursement_records(id)
);
