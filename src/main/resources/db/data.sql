-- =====================================================================
-- MoveGuard seed data
-- 1) 위험 요인·진단 규칙 (기준 데이터)
-- 2) 더미 이전사업 1건 (공인IP 리스크 고위험 케이스)
-- IP는 문서용 예약 대역(203.0.113.0/24, 198.51.100.0/24)만 사용
-- =====================================================================
SET NAMES utf8mb4;

INSERT INTO risk_factor (code, name, weight, description) VALUES
('NETWORK_IP', '공인IP·네트워크 변경', 0.350, '접속 주소 변경, 외부 허용목록, 대역 충돌'),
('DNS',        'DNS 전환',             0.150, '레코드 변경 누락, TTL'),
('COMPAT',     'OS·DBMS 호환성',       0.200, '버전·라이브러리 호환성'),
('BACKUP',     '백업·복구',            0.150, '백업 성공, 복구 테스트'),
('SECURITY',   '보안·접근통제',        0.150, '방화벽·포트, 인증서, 계정 권한');

INSERT INTO risk_rule
    (rule_code, factor_id, title, severity, occurrence, detection, is_blocking, message_template, mitigation)
VALUES
('IP-01',
 (SELECT factor_id FROM risk_factor WHERE code = 'NETWORK_IP'),
 '공인IP로 접속 중인 대상의 공인IP가 이전 후 변경됨',
 9, 8, 6, 1,
 '{asset}이(가) {target}에 공인IP {address}:{port}로 접속하고 있으나, 이전 후 해당 공인IP가 변경됩니다. 설정 위치: {config}',
 '전환 전 접속 주소를 신규 IP 또는 내부 도메인으로 수정하고, 수정된 설정으로 연결 테스트를 완료하십시오.'),
('IP-02',
 (SELECT factor_id FROM risk_factor WHERE code = 'NETWORK_IP'),
 '내부 서버 간 통신에 공인IP 사용',
 6, 6, 5, 0,
 '{asset}에서 내부 서버 {target}로의 통신이 공인IP {address}를 경유합니다.',
 '같은 네트워크 내 통신은 사설IP 또는 내부 도메인으로 변경해 외부 노출과 IP 변경 영향을 줄이십시오.'),
('IP-03',
 (SELECT factor_id FROM risk_factor WHERE code = 'NETWORK_IP'),
 '접속 주소가 IP로 하드코딩됨',
 5, 7, 6, 0,
 '{asset}의 {config}에 접속 주소가 IP({address})로 직접 기재되어 있습니다.',
 '도메인 기반 접속으로 전환하면 이후 IP 변경 시 설정 수정 없이 DNS 변경만으로 대응할 수 있습니다.'),
('IP-04',
 (SELECT factor_id FROM risk_factor WHERE code = 'NETWORK_IP'),
 '외부 기관 방화벽 허용목록에 등록된 공인IP 변경',
 9, 7, 8, 1,
 '{asset}의 공인IP {address}가 외부 기관 방화벽 허용목록에 등록되어 있으나 이전 후 변경됩니다.',
 '외부 기관에 신규 공인IP 허용 등록을 사전 요청하고, 처리 기간을 전환 일정에 반영하십시오.'),
('IP-05',
 (SELECT factor_id FROM risk_factor WHERE code = 'NETWORK_IP'),
 '이전 후 IP 미확정',
 7, 5, 4, 0,
 '{asset}의 이전 후 IP 정보가 등록되지 않았습니다.',
 '신규 환경의 IP 할당 계획을 확정한 뒤 재진단하십시오.'),
('DNS-01',
 (SELECT factor_id FROM risk_factor WHERE code = 'DNS'),
 '변경되는 공인IP를 가리키는 레코드의 TTL이 김',
 6, 7, 5, 0,
 '{domain} A 레코드가 변경 예정 IP {address}를 가리키며 TTL이 {ttl}초입니다.',
 '전환 최소 1 TTL 전에 TTL을 300초 이하로 단축해 캐시 전파 지연을 줄이십시오.'),
('DNS-02',
 (SELECT factor_id FROM risk_factor WHERE code = 'DNS'),
 '변경되는 공인IP에 대한 DNS 변경 계획 누락',
 8, 6, 6, 1,
 '{domain} A 레코드가 변경 예정 IP {address}를 가리키지만 이전 후 레코드가 등록되지 않았습니다.',
 '이전 후 레코드 값을 확정하고 변경 담당자와 시점을 전환 계획에 포함하십시오.');

INSERT INTO attribute_def (attr_key, data_type, description) VALUES
('maintenance_window',  'STRING', '작업 가능 시간대'),
('service_criticality', 'NUMBER', '서비스 중요도 1~5'),
('data_size_gb',        'NUMBER', '이전 데이터 용량(GB)'),
('owner_contact',       'STRING', '자산 담당자 연락처');

-- 더미 이전사업: 쇼핑몰 IDC → AWS 이전
-- 기대 진단 결과: IP-01, IP-02, IP-03, IP-04, DNS-01 / 전환 차단
INSERT INTO migration_project (name, customer_name, source_env, target_env, status, planned_date)
VALUES ('쇼핑몰 서비스 클라우드 이전', '가나다커머스', 'IDC-A', 'AWS ap-northeast-2', 'PLAN', '2026-10-24');

SET @p := LAST_INSERT_ID();

INSERT INTO asset (project_id, name, asset_type, role, os_name, os_version) VALUES
(@p, 'web01',  'SERVER',   'WEB', 'Rocky Linux', '8.9'),
(@p, 'db01',   'SERVER',   'DB',  'Rocky Linux', '8.9'),
(@p, 'pg-api', 'EXTERNAL', NULL,  NULL,          NULL);

SET @web := (SELECT asset_id FROM asset WHERE project_id = @p AND name = 'web01');
SET @db  := (SELECT asset_id FROM asset WHERE project_id = @p AND name = 'db01');
SET @pg  := (SELECT asset_id FROM asset WHERE project_id = @p AND name = 'pg-api');

INSERT INTO asset_ip (asset_id, address, ip_type, phase, ext_whitelisted, note) VALUES
(@web, '10.10.1.11',    'PRIVATE', 'BEFORE', 0, NULL),
(@web, '203.0.113.11',  'PUBLIC',  'BEFORE', 1, 'PG사 방화벽 허용목록 등록'),
(@web, '172.31.10.11',  'PRIVATE', 'AFTER',  0, NULL),
(@web, '198.51.100.21', 'PUBLIC',  'AFTER',  0, 'EIP 예정'),
(@db,  '10.10.1.21',    'PRIVATE', 'BEFORE', 0, NULL),
(@db,  '203.0.113.21',  'PUBLIC',  'BEFORE', 0, NULL),
(@db,  '172.31.20.21',  'PRIVATE', 'AFTER',  0, NULL);

INSERT INTO dependency (project_id, from_asset_id, to_asset_id, target_address, target_port, protocol, config_location) VALUES
(@p, @web, @db, '203.0.113.21',       3306, 'JDBC',  'application.yml spring.datasource.url'),
(@p, @web, @pg, 'api.pg-example.com', 443,  'HTTPS', 'application.yml pg.api.base-url');

INSERT INTO dns_record (project_id, domain, record_type, value, ttl, phase) VALUES
(@p, 'shop.example.com', 'A', '203.0.113.11',  3600, 'BEFORE'),
(@p, 'shop.example.com', 'A', '198.51.100.21', 300,  'AFTER');

INSERT INTO asset_attribute (asset_id, attr_key, attr_value) VALUES
(@web, 'service_criticality', '5'),
(@web, 'maintenance_window',  '토 02:00~06:00'),
(@db,  'data_size_gb',        '120');
