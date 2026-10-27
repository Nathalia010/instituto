CREATE TABLE IF NOT EXISTS demo_students (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(180) NOT NULL,
  email VARCHAR(254) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  program VARCHAR(50) NOT NULL,
  document VARCHAR(30) NOT NULL UNIQUE,
  issued VARCHAR(10) NOT NULL,
  academic_year INTEGER NOT NULL,
  key_hash VARCHAR(100) NOT NULL,
  academic_status VARCHAR(40) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS demo_inquiries (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(180) NOT NULL,
  email VARCHAR(254) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  program VARCHAR(50) NOT NULL,
  message VARCHAR(2000) NOT NULL,
  consent BOOLEAN NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'NUEVA',
  student_id VARCHAR(36) REFERENCES demo_students(id),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS demo_notes (
  id VARCHAR(36) PRIMARY KEY,
  student_id VARCHAR(36) NOT NULL REFERENCES demo_students(id),
  subject VARCHAR(120) NOT NULL,
  grade NUMERIC(5,2) NOT NULL,
  academic_year INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS demo_movements (
  id VARCHAR(36) PRIMARY KEY,
  student_id VARCHAR(36) NOT NULL REFERENCES demo_students(id),
  concept VARCHAR(120) NOT NULL,
  type VARCHAR(10) NOT NULL CHECK(type IN ('COBRO','ABONO')),
  amount NUMERIC(14,2) NOT NULL CHECK(amount > 0),
  movement_date DATE NOT NULL
);
CREATE TABLE IF NOT EXISTS demo_audit (
  id VARCHAR(36) PRIMARY KEY,
  actor VARCHAR(80) NOT NULL,
  action VARCHAR(80) NOT NULL,
  resource_id VARCHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE demo_students ADD COLUMN IF NOT EXISTS admin_notes VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE demo_inquiries ADD COLUMN IF NOT EXISTS document VARCHAR(30) NOT NULL DEFAULT '';
ALTER TABLE demo_inquiries ADD COLUMN IF NOT EXISTS admin_notes VARCHAR(2000) NOT NULL DEFAULT '';
ALTER TABLE demo_inquiries ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE IF NOT EXISTS demo_data_records (
  module VARCHAR(30) NOT NULL,
  record_key VARCHAR(200) NOT NULL,
  academic_year INTEGER,
  payload CLOB NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(module, record_key)
);
CREATE TABLE IF NOT EXISTS demo_data_previews (
  id VARCHAR(36) PRIMARY KEY,
  session_hash VARCHAR(64) NOT NULL,
  module VARCHAR(30) NOT NULL,
  filename VARCHAR(255) NOT NULL,
  payload CLOB NOT NULL,
  scale_min NUMERIC(10,2) NOT NULL,
  scale_max NUMERIC(10,2) NOT NULL,
  confirmed BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS demo_data_lock (id INTEGER PRIMARY KEY);
MERGE INTO demo_data_lock (id) KEY(id) VALUES (1);
CREATE TABLE IF NOT EXISTS demo_gallery (
  id VARCHAR(36) PRIMARY KEY,
  caption VARCHAR(180) NOT NULL,
  mime VARCHAR(30) NOT NULL,
  image BLOB,
  built_in BOOLEAN NOT NULL DEFAULT FALSE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS demo_content_seeds (id VARCHAR(80) PRIMARY KEY);
INSERT INTO demo_gallery(id,caption,mime,built_in)
SELECT 'perro-centro-saber','Estudiar abre nuevos caminos','image/png',TRUE
WHERE NOT EXISTS (SELECT 1 FROM demo_gallery WHERE id='perro-centro-saber') AND NOT EXISTS (SELECT 1 FROM demo_content_seeds WHERE id='gallery-initialized');
MERGE INTO demo_content_seeds(id) KEY(id) VALUES ('gallery-initialized');
CREATE TABLE IF NOT EXISTS demo_registration_settings (
 id INTEGER PRIMARY KEY,
 form1 VARCHAR(500) NOT NULL,
 form2 VARCHAR(500) NOT NULL,
 neli_name VARCHAR(80) NOT NULL,
 neli_phone VARCHAR(10) NOT NULL,
 estiven_name VARCHAR(80) NOT NULL,
 estiven_phone VARCHAR(10) NOT NULL
);
INSERT INTO demo_registration_settings(id,form1,form2,neli_name,neli_phone,estiven_name,estiven_phone)
SELECT 1,'https://docs.google.com/forms/d/e/1FAIpQLSe4Eb7DtGo7VTpfMRlwrfEf4GrMhsctWW8uvLjDmWxECUL8yw/viewform','https://docs.google.com/forms/d/e/1FAIpQLSdaVqATplzwGEqTBARjY_bN0LE5zgrFNZ_eOUN2toy9FzRCZA/viewform','Neli','3045822782','Estiven','3143544501'
WHERE NOT EXISTS (SELECT 1 FROM demo_registration_settings WHERE id=1);

CREATE TABLE IF NOT EXISTS demo_event_images (
 code VARCHAR(60) PRIMARY KEY,
 mime VARCHAR(30) NOT NULL,
 image BLOB NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE demo_data_records ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE demo_inquiries ADD COLUMN IF NOT EXISTS first_name VARCHAR(80) NOT NULL DEFAULT '';
ALTER TABLE demo_inquiries ADD COLUMN IF NOT EXISTS last_name VARCHAR(90) NOT NULL DEFAULT '';
ALTER TABLE demo_inquiries ADD COLUMN IF NOT EXISTS city VARCHAR(120) NOT NULL DEFAULT '';
ALTER TABLE demo_inquiries ADD COLUMN IF NOT EXISTS neighborhood VARCHAR(120) NOT NULL DEFAULT '';
CREATE TABLE IF NOT EXISTS demo_sites (
 id INTEGER PRIMARY KEY,
 name VARCHAR(120) NOT NULL,
 address VARCHAR(300) NOT NULL DEFAULT '',
 phone VARCHAR(10) NOT NULL DEFAULT '',
 hours VARCHAR(300) NOT NULL DEFAULT '',
 map_url VARCHAR(500) NOT NULL DEFAULT '',
 mime VARCHAR(30),
 image BLOB,
 image_active BOOLEAN NOT NULL DEFAULT FALSE
);
INSERT INTO demo_sites(id,name,address,phone,hours,map_url) SELECT 1,'Sede 1','Calle 26 B # 12i – 22 Sur · Barrio San José','3143544501','Martes a domingo, de 10:00 a. m. a 6:00 p. m.','https://maps.app.goo.gl/4wfy6bWvpJBwGf549' WHERE NOT EXISTS (SELECT 1 FROM demo_sites WHERE id=1);
INSERT INTO demo_sites(id,name) SELECT 2,'Sede 2' WHERE NOT EXISTS (SELECT 1 FROM demo_sites WHERE id=2);
INSERT INTO demo_sites(id,name) SELECT 3,'Sede 3' WHERE NOT EXISTS (SELECT 1 FROM demo_sites WHERE id=3);
ALTER TABLE demo_audit ALTER COLUMN resource_id VARCHAR(200);

CREATE TABLE IF NOT EXISTS demo_levelings (
 id VARCHAR(36) PRIMARY KEY,
 student_id VARCHAR(36) NOT NULL REFERENCES demo_students(id),
 subject VARCHAR(40) NOT NULL,
 period VARCHAR(80) NOT NULL,
 academic_year INTEGER NOT NULL,
 reason VARCHAR(500) NOT NULL,
 preferred_date VARCHAR(10) NOT NULL,
 preferred_time VARCHAR(5) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
 teacher VARCHAR(180) NOT NULL DEFAULT '',
 location VARCHAR(300) NOT NULL DEFAULT '',
 confirmed_date VARCHAR(10) NOT NULL DEFAULT '',
 confirmed_time VARCHAR(5) NOT NULL DEFAULT '',
 result VARCHAR(500) NOT NULL DEFAULT '',
 response VARCHAR(500) NOT NULL DEFAULT '',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE demo_gallery ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE demo_levelings ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE demo_levelings ADD COLUMN IF NOT EXISTS slot_id VARCHAR(36);
CREATE TABLE IF NOT EXISTS demo_leveling_slots (
 id VARCHAR(36) PRIMARY KEY,
 slot_date VARCHAR(10) NOT NULL,
 slot_time VARCHAR(5) NOT NULL,
 subject VARCHAR(40) NOT NULL,
 teacher VARCHAR(180) NOT NULL,
 location VARCHAR(300) NOT NULL,
 capacity INTEGER NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS demo_publications (
 kind VARCHAR(20) NOT NULL,
 id VARCHAR(60) NOT NULL,
 payload CLOB NOT NULL,
 deleted BOOLEAN NOT NULL DEFAULT FALSE,
 PRIMARY KEY(kind,id)
);
ALTER TABLE demo_event_images ALTER COLUMN code VARCHAR(160);

ALTER TABLE demo_students ADD COLUMN IF NOT EXISTS account_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE demo_students ADD COLUMN IF NOT EXISTS account_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE demo_students ADD COLUMN IF NOT EXISTS document_type VARCHAR(6) NOT NULL DEFAULT 'CC';

ALTER TABLE demo_students ALTER COLUMN account_active SET DEFAULT FALSE;

ALTER TABLE demo_students ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE demo_data_records ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS demo_course_grades (
 course_code VARCHAR(50) NOT NULL,
 student_id VARCHAR(36) NOT NULL REFERENCES demo_students(id),
 academic_year INTEGER NOT NULL,
 subject_key VARCHAR(180) NOT NULL,
 subject_name VARCHAR(180) NOT NULL,
 grade NUMERIC(3,2),
 PRIMARY KEY(course_code,student_id,academic_year,subject_key),
 CHECK(grade IS NULL OR (grade >= 0 AND grade <= 5))
);

ALTER TABLE demo_levelings ADD COLUMN IF NOT EXISTS course_code VARCHAR(50) NOT NULL DEFAULT '';
ALTER TABLE demo_levelings ADD COLUMN IF NOT EXISTS course_name VARCHAR(180) NOT NULL DEFAULT '';
ALTER TABLE demo_levelings ADD COLUMN IF NOT EXISTS subject_name VARCHAR(180) NOT NULL DEFAULT '';
ALTER TABLE demo_levelings ALTER COLUMN subject VARCHAR(180);
ALTER TABLE demo_leveling_slots ALTER COLUMN subject VARCHAR(180);

ALTER TABLE demo_sites ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE demo_leveling_slots ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
DELETE FROM demo_gallery WHERE deleted=TRUE;
