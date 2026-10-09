SET @cmp01 := (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-01');
INSERT INTO migration_project (name, customer_name, source_env, target_env, status, planned_date) VALUES ('바사증권 트레이딩 시스템','바사증권','IDC','IDC · RHEL 7','PLAN','2026-10-09');
SET @p := LAST_INSERT_ID();
INSERT INTO diagnosis_run (project_id, total_score, risk_level, blocked, finding_count, executed_at) VALUES (@p, 18.14, 'HIGH', 1, 11, '2026-08-10 10:12:00');
SET @r := LAST_INSERT_ID();
INSERT INTO diagnosis_finding (run_id, rule_id, rpn, detail) VALUES
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-01'), 504, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-02'), 420, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-04'), 390, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-07'), 300, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-01'), 270, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-02'), 240, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CERT-01'), 210, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CERT-02'), 200, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='BAK-01'), 192, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='BAK-02'), 180, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='BAK-03'), 120, '데모 샘플 리스크');
INSERT INTO diagnosis_run (project_id, total_score, risk_level, blocked, finding_count, executed_at) VALUES (@p, 16.2, 'HIGH', 1, 10, '2026-09-09 11:03:00');
SET @r := LAST_INSERT_ID();
INSERT INTO diagnosis_finding (run_id, rule_id, rpn, detail) VALUES
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-01'), 450, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-02'), 420, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-04'), 300, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-07'), 270, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-01'), 240, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-02'), 210, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CERT-01'), 200, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CERT-02'), 192, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='BAK-01'), 150, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='BAK-02'), 120, '데모 샘플 리스크');
INSERT INTO diagnosis_run (project_id, total_score, risk_level, blocked, finding_count, executed_at) VALUES (@p, 15.12, 'HIGH', 1, 9, '2026-10-08 09:41:00');
SET @r := LAST_INSERT_ID();
INSERT INTO diagnosis_finding (run_id, rule_id, rpn, detail) VALUES
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-01'), 420, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-02'), 390, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-04'), 270, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-07'), 240, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-01'), 210, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-02'), 200, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CERT-01'), 192, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CERT-02'), 150, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='BAK-01'), 120, '데모 샘플 리스크');
INSERT INTO migration_project (name, customer_name, source_env, target_env, status, planned_date) VALUES ('라마마트 물류 플랫폼','라마마트','IDC','IDC · Rocky Linux 8','PLAN','2026-10-09');
SET @p := LAST_INSERT_ID();
INSERT INTO diagnosis_run (project_id, total_score, risk_level, blocked, finding_count, executed_at) VALUES (@p, 15.12, 'HIGH', 1, 8, '2026-08-12 14:20:00');
SET @r := LAST_INSERT_ID();
INSERT INTO diagnosis_finding (run_id, rule_id, rpn, detail) VALUES
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-01'), 420, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-02'), 300, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-04'), 270, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-07'), 240, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-01'), 210, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-02'), 200, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CERT-01'), 150, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CERT-02'), 120, '데모 샘플 리스크');
INSERT INTO diagnosis_run (project_id, total_score, risk_level, blocked, finding_count, executed_at) VALUES (@p, 10.8, 'MEDIUM', 0, 6, '2026-09-11 15:00:00');
SET @r := LAST_INSERT_ID();
INSERT INTO diagnosis_finding (run_id, rule_id, rpn, detail) VALUES
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-01'), 300, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-02'), 270, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-04'), 240, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-07'), 210, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-01'), 200, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-02'), 120, '데모 샘플 리스크');
INSERT INTO diagnosis_run (project_id, total_score, risk_level, blocked, finding_count, executed_at) VALUES (@p, 9.72, 'MEDIUM', 0, 5, '2026-10-07 16:22:00');
SET @r := LAST_INSERT_ID();
INSERT INTO diagnosis_finding (run_id, rule_id, rpn, detail) VALUES
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-01'), 270, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-02'), 240, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-04'), 210, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-07'), 192, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-01'), 120, '데모 샘플 리스크');
INSERT INTO migration_project (name, customer_name, source_env, target_env, status, planned_date) VALUES ('아자카드 결제 게이트웨이','아자카드','IDC','IDC · Ubuntu 22.04','PLAN','2026-10-09');
SET @p := LAST_INSERT_ID();
INSERT INTO diagnosis_run (project_id, total_score, risk_level, blocked, finding_count, executed_at) VALUES (@p, 8.64, 'MEDIUM', 0, 5, '2026-08-15 09:00:00');
SET @r := LAST_INSERT_ID();
INSERT INTO diagnosis_finding (run_id, rule_id, rpn, detail) VALUES
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-01'), 240, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-02'), 200, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-04'), 180, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-07'), 150, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-01'), 120, '데모 샘플 리스크');
INSERT INTO diagnosis_run (project_id, total_score, risk_level, blocked, finding_count, executed_at) VALUES (@p, 6.48, 'MEDIUM', 0, 3, '2026-09-14 09:30:00');
SET @r := LAST_INSERT_ID();
INSERT INTO diagnosis_finding (run_id, rule_id, rpn, detail) VALUES
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-01'), 180, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-02'), 150, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-04'), 120, '데모 샘플 리스크');
INSERT INTO diagnosis_run (project_id, total_score, risk_level, blocked, finding_count, executed_at) VALUES (@p, 4.32, 'LOW', 0, 2, '2026-10-06 10:05:00');
SET @r := LAST_INSERT_ID();
INSERT INTO diagnosis_finding (run_id, rule_id, rpn, detail) VALUES
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-01'), 120, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-02'), 90, '데모 샘플 리스크');
INSERT INTO migration_project (name, customer_name, source_env, target_env, status, planned_date) VALUES ('차타병원 EMR','차타병원','IDC','IDC · Windows Server 2016','PLAN','2026-10-09');
SET @p := LAST_INSERT_ID();
INSERT INTO diagnosis_run (project_id, total_score, risk_level, blocked, finding_count, executed_at) VALUES (@p, 14.04, 'MEDIUM', 0, 7, '2026-08-18 13:10:00');
SET @r := LAST_INSERT_ID();
INSERT INTO diagnosis_finding (run_id, rule_id, rpn, detail) VALUES
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-01'), 390, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-02'), 310, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-04'), 270, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-07'), 240, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-01'), 210, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-02'), 200, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CERT-01'), 120, '데모 샘플 리스크');
INSERT INTO diagnosis_run (project_id, total_score, risk_level, blocked, finding_count, executed_at) VALUES (@p, 11.16, 'MEDIUM', 0, 5, '2026-09-16 13:40:00');
SET @r := LAST_INSERT_ID();
INSERT INTO diagnosis_finding (run_id, rule_id, rpn, detail) VALUES
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-01'), 310, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-02'), 270, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-04'), 240, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-07'), 210, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='PORT-01'), 120, '데모 샘플 리스크');
INSERT INTO diagnosis_run (project_id, total_score, risk_level, blocked, finding_count, executed_at) VALUES (@p, 9.72, 'MEDIUM', 0, 4, '2026-10-05 14:15:00');
SET @r := LAST_INSERT_ID();
INSERT INTO diagnosis_finding (run_id, rule_id, rpn, detail) VALUES
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-01'), 270, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-02'), 240, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-04'), 210, '데모 샘플 리스크'),
(@r, (SELECT rule_id FROM risk_rule WHERE rule_code='CMP-07'), 120, '데모 샘플 리스크');
