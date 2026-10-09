-- =====================================================================
-- 호환성 규칙 + 기준 데이터 초기값 + 더미 사업 소프트웨어 정보
-- compat_release는 동기화로 갱신되며, is_manual = 1인 행은 유지된다.
-- =====================================================================
SET NAMES utf8mb4;

-- 1. 진단 규칙 (COMPAT 요인)
INSERT INTO risk_rule
    (rule_code, factor_id, title, severity, occurrence, detection, is_blocking, message_template, mitigation)
VALUES
('CMP-01',
 (SELECT factor_id FROM risk_factor WHERE code = 'COMPAT'),
 '버전이 이미 지원 종료(EOL)됨',
 10, 7, 6, 1,
 '{asset}의 {product} {version}은(는) {eol}에 지원이 종료되었습니다. 보안 패치를 받을 수 없습니다.',
 '지원 중인 버전으로 목표 버전을 변경하거나, 연장 지원 계약 여부를 확인한 뒤 조치하십시오.'),

('CMP-02',
 (SELECT factor_id FROM risk_factor WHERE code = 'COMPAT'),
 '목표 WAS가 요구하는 최소 Java 버전 미달',
 9, 6, 5, 1,
 '{asset}의 {product} {version}은(는) Java {required} 이상이 필요하지만, Java 버전은 {current}입니다.',
 'Java 런타임을 필요한 버전 이상으로 올리거나, 현재 Java 버전을 지원하는 WAS 버전으로 목표를 조정하십시오.'),

('CMP-03',
 (SELECT factor_id FROM risk_factor WHERE code = 'COMPAT'),
 'Tomcat 10 이상 전환으로 javax → jakarta 패키지 변경 필요',
 9, 7, 6, 1,
 '{asset}이(가) {product} {from}에서 {to}(으)로 이전합니다. Tomcat 10부터 서블릿 패키지가 javax에서 jakarta로 변경되어 애플리케이션 수정 없이는 기동되지 않습니다.',
 '애플리케이션의 javax.servlet 참조를 jakarta.servlet로 변경하고, 의존 라이브러리의 Jakarta 대응 버전을 확인하십시오.'),

('CMP-04',
 (SELECT factor_id FROM risk_factor WHERE code = 'COMPAT'),
 '버전의 지원 종료가 임박함',
 6, 6, 4, 0,
 '{asset}의 {product} {version}은(는) {eol}에 지원이 종료됩니다. 전환 후 1년 이내에 재업그레이드가 필요합니다.',
 '지원 기간이 더 긴 LTS 버전을 목표로 검토하십시오.'),

('CMP-05',
 (SELECT factor_id FROM risk_factor WHERE code = 'COMPAT'),
 '메이저 버전을 여러 단계 건너뜀',
 7, 5, 5, 0,
 '{asset}의 {product}이(가) {from}에서 {to}(으)로 이전합니다. 중간 버전을 건너뛰는 업그레이드는 데이터 변환·문법 변경 위험이 있습니다.',
 '벤더가 권장하는 업그레이드 경로를 확인하고, 필요하면 단계적 업그레이드로 계획하십시오.'),

('CMP-06',
 (SELECT factor_id FROM risk_factor WHERE code = 'COMPAT'),
 '버전이 연장 지원(유상) 구간임',
 5, 5, 3, 0,
 '{asset}의 {product} {version}은(는) 활성(일반) 지원이 종료되어 {extSupport}까지 연장 지원 구간입니다. 기술 지원은 가능하나 추가 비용·제약이 따릅니다.',
 '지원 기간이 더 긴 상위 버전으로 목표를 조정하거나, 연장 지원 계약 비용을 운영 계획에 반영하십시오.'),

('CMP-07',
 (SELECT factor_id FROM risk_factor WHERE code = 'COMPAT'),
 'JDBC 드라이버 버전이 목표 DB 요구치 미달',
 8, 5, 5, 1,
 '{asset}의 {driver} {version}은(는) 목표 DB({db})가 요구하는 최소 드라이버 버전({required})보다 낮습니다. 전환 후 DB 접속이 실패할 수 있습니다.',
 '드라이버를 목표 DB가 요구하는 최소 버전 이상으로 올린 뒤 조치하십시오.');

-- 2. 동기화 대상 제품
INSERT INTO compat_product (product, label, category, version_command, source_url, fetch_status) VALUES
('tomcat',          'Apache Tomcat',   'server-app', './bin/version.sh',                    'https://endoflife.date/tomcat',          'PENDING'),
-- java는 endoflife.date에 통합 제품이 없고 배포판(oracle-jdk 등)으로 나뉘므로 동기화 대상에서 제외(MANUAL)
('java',            'Java (JDK)',      'lang',       'java -version',                       'https://endoflife.date/',                'MANUAL'),
('mysql',           'MySQL',           'database',   'mysqld --version',                    'https://endoflife.date/mysql',           'PENDING'),
('oracle-database', 'Oracle Database', 'database',   'SELECT BANNER_FULL FROM V$VERSION;',  'https://endoflife.date/oracle-database', 'PENDING'),
('postgresql',      'PostgreSQL',      'database',   'postgres --version',                  'https://endoflife.date/postgresql',      'PENDING'),
('nginx',           'nginx',           'server-app', 'nginx -v',                            'https://endoflife.date/nginx',           'PENDING'),
-- OS (IDC → 클라우드 이전에서 지원 종료·업그레이드 경로 진단용)
('rocky-linux',     'Rocky Linux',     'os',         'cat /etc/os-release',                 'https://endoflife.date/rocky-linux',     'PENDING'),
('rhel',            'RHEL',            'os',         'cat /etc/redhat-release',             'https://endoflife.date/rhel',            'PENDING'),
('centos',          'CentOS',          'os',         'cat /etc/centos-release',             'https://endoflife.date/centos',          'PENDING'),
('ubuntu',          'Ubuntu',          'os',         'lsb_release -a',                      'https://endoflife.date/ubuntu',          'PENDING'),
('debian',          'Debian',          'os',         'cat /etc/debian_version',             'https://endoflife.date/debian',          'PENDING'),
('amazon-linux',    'Amazon Linux',    'os',         'cat /etc/os-release',                 'https://endoflife.date/amazon-linux',    'PENDING'),
('almalinux',       'AlmaLinux',       'os',         'cat /etc/os-release',                 'https://endoflife.date/almalinux',       'PENDING'),
('windows-server',  'Windows Server',  'os',         'systeminfo',                          'https://endoflife.date/windows-server',  'PENDING');

-- 3. 기준 데이터 초기값 (동기화 전에도 진단이 동작하도록)
INSERT INTO compat_release
    (product, version, label, is_lts, is_eol, eol_date, ext_support_date, is_maintained, min_java_version) VALUES
('tomcat', '11.0', '11.0', 0, 0, NULL,         NULL, 1, '17'),
('tomcat', '10.1', '10.1', 0, 0, NULL,         NULL, 1, '11'),
('tomcat', '10.0', '10.0', 0, 1, '2022-10-31', NULL, 0, '8'),
('tomcat', '9.0',  '9.0',  0, 0, '2027-03-31', NULL, 1, '8'),
('mysql',  '9.7',  '9.7 (LTS)', 1, 0, '2034-04-21', NULL, 1, NULL),
('mysql',  '9.6',  '9.6',       0, 1, '2026-04-21', NULL, 0, NULL),
('mysql',  '8.4',  '8.4 (LTS)', 1, 0, '2032-04-30', NULL, 1, NULL),
('mysql',  '5.7',  '5.7',       0, 1, '2023-10-31', NULL, 0, NULL),
('oracle-database', '19',   '19c (LTR)',     1, 0, '2029-12-31', '2032-12-31', 1, NULL),
('oracle-database', '12.2', '12c Release 2', 0, 1, '2022-03-31', NULL,         0, NULL);

-- 3b. JDBC 드라이버 호환 매트릭스 (endoflife.date 미제공 → 수동 관리, is_manual=1)
INSERT INTO driver_requirement (db_product, db_release_line, driver_product, min_version, note) VALUES
('mysql',           '8.4',  'mysql-connector-j',    '8.4.0',  'MySQL 8.4 LTS는 Connector/J 8.4 이상 권장'),
('mysql',           '8.0',  'mysql-connector-j',    '8.0.11', 'MySQL 8.0은 Connector/J 8.0.11 이상'),
('mysql',           '9.6',  'mysql-connector-j',    '9.0.0',  'MySQL 9.x(이노베이션)는 Connector/J 9.x'),
('mysql',           NULL,   'mariadb-java-client',  '2.7.0',  'MariaDB 드라이버로 MySQL 접속 시'),
('oracle-database', '19',   'ojdbc8',               '19.3',   'Oracle 19c + JDK 8'),
('oracle-database', '19',   'ojdbc10',              '19.3',   'Oracle 19c + JDK 10/11'),
('oracle-database', '21',   'ojdbc11',              '21.1',   'Oracle 21c + JDK 11'),
('postgresql',      NULL,   'postgresql',           '42.2.0', 'PostgreSQL JDBC 42.2 이상 권장');

-- 4. 더미 이전사업에 소프트웨어 정보 추가
--    기대 결과: CMP-01(mysql 9.6 EOL), CMP-02(Tomcat 11은 Java 17 필요), CMP-03(9.0→11.0),
--             CMP-05(5.7→9.6), CMP-07(mysql 9.6은 connector-j 9.0+ 필요한데 8.0.33 미달)
SET @p := (SELECT project_id FROM migration_project ORDER BY project_id LIMIT 1);
SET @web := (SELECT asset_id FROM asset WHERE project_id = @p AND name = 'web01');
SET @db  := (SELECT asset_id FROM asset WHERE project_id = @p AND name = 'db01');

INSERT INTO asset_software (asset_id, product, role, version, release_line, phase) VALUES
(@web, 'tomcat',      'WAS',     '9.0.80',    '9.0',  'BEFORE'),
(@web, 'java',        'RUNTIME', '1.8.0_382', '8',    'BEFORE'),
(@web, 'rocky-linux', 'OS',      '8.9',       '8',    'BEFORE'),
(@db,  'mysql',       'DB',      '5.7.44',    '5.7',  'BEFORE'),
(@db,  'rocky-linux', 'OS',      '8.9',       '8',    'BEFORE');

INSERT INTO asset_software (asset_id, product, role, version, release_line, phase) VALUES
(@web, 'tomcat',            'WAS',     '11.0.22',   '11.0', 'AFTER'),
(@web, 'java',              'RUNTIME', '11.0.24',   '11',   'AFTER'),
(@web, 'mysql-connector-j', 'DRIVER',  '8.0.33',    '8.0',  'AFTER'),
(@web, 'rocky-linux',       'OS',      '9.4',       '9',    'AFTER'),
(@db,  'mysql',             'DB',      '9.6.1',     '9.6',  'AFTER'),
(@db,  'rocky-linux',       'OS',      '9.4',       '9',    'AFTER');
