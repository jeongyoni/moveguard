-- =====================================================================
-- MoveGuard schema (MySQL 8.0)
-- 1차 범위: 이전사업 입력 → 공인IP·DNS 진단 → 위험도 산정
-- 전환 실행·검증·롤백 테이블은 2차 범위에서 추가
-- =====================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS diagnosis_finding;
DROP TABLE IF EXISTS diagnosis_run;
DROP TABLE IF EXISTS risk_rule;
DROP TABLE IF EXISTS risk_factor;
DROP TABLE IF EXISTS dns_record;
DROP TABLE IF EXISTS dependency;
DROP TABLE IF EXISTS asset_attribute;
DROP TABLE IF EXISTS attribute_def;
DROP TABLE IF EXISTS asset_ip;
DROP TABLE IF EXISTS asset;
DROP TABLE IF EXISTS migration_project;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE migration_project (
    project_id     BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100) NOT NULL,
    customer_name  VARCHAR(100) NOT NULL,
    source_env     VARCHAR(100)          COMMENT '기존 환경 (예: IDC-A)',
    target_env     VARCHAR(100)          COMMENT '신규 환경 (예: AWS ap-northeast-2)',
    status         VARCHAR(20)  NOT NULL DEFAULT 'PLAN',
    planned_date   DATE,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id),
    CONSTRAINT chk_project_status CHECK (status IN ('PLAN', 'READY', 'BLOCKED', 'DONE'))
) ENGINE = InnoDB COMMENT = '이전사업';

CREATE TABLE asset (
    asset_id    BIGINT       NOT NULL AUTO_INCREMENT,
    project_id  BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    asset_type  VARCHAR(20)  NOT NULL COMMENT 'SERVER: 이전 대상 / EXTERNAL: 외부 기관·API',
    role        VARCHAR(20)           COMMENT 'WEB / WAS / DB / ETC',
    os_name     VARCHAR(50),
    os_version  VARCHAR(30),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (asset_id),
    UNIQUE KEY uk_asset_name (project_id, name),
    CONSTRAINT fk_asset_project FOREIGN KEY (project_id)
        REFERENCES migration_project (project_id) ON DELETE CASCADE,
    CONSTRAINT chk_asset_type CHECK (asset_type IN ('SERVER', 'EXTERNAL')),
    CONSTRAINT chk_asset_role CHECK (role IS NULL OR role IN ('WEB', 'WAS', 'DB', 'ETC'))
) ENGINE = InnoDB COMMENT = '이전 대상 자산';

CREATE TABLE asset_ip (
    ip_id            BIGINT      NOT NULL AUTO_INCREMENT,
    asset_id         BIGINT      NOT NULL,
    address          VARCHAR(45) NOT NULL COMMENT 'IPv4/IPv6',
    ip_type          VARCHAR(10) NOT NULL COMMENT 'PUBLIC / PRIVATE',
    phase            VARCHAR(10) NOT NULL COMMENT 'BEFORE: 이전 전 / AFTER: 이전 후',
    ext_whitelisted  TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '외부 기관 방화벽 허용목록 등록 여부',
    note             VARCHAR(255),
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ip_id),
    UNIQUE KEY uk_asset_ip (asset_id, address, phase),
    KEY idx_asset_ip_lookup (address, phase),
    CONSTRAINT fk_ip_asset FOREIGN KEY (asset_id)
        REFERENCES asset (asset_id) ON DELETE CASCADE,
    CONSTRAINT chk_ip_type CHECK (ip_type IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT chk_ip_phase CHECK (phase IN ('BEFORE', 'AFTER'))
) ENGINE = InnoDB COMMENT = '자산별 IP (이전 전/후)';

CREATE TABLE attribute_def (
    attr_key     VARCHAR(50)  NOT NULL,
    data_type    VARCHAR(10)  NOT NULL COMMENT 'STRING / NUMBER / BOOL / DATE',
    description  VARCHAR(255),
    PRIMARY KEY (attr_key),
    CONSTRAINT chk_attr_type CHECK (data_type IN ('STRING', 'NUMBER', 'BOOL', 'DATE'))
) ENGINE = InnoDB COMMENT = '확장 속성 정의';

CREATE TABLE asset_attribute (
    attr_id     BIGINT       NOT NULL AUTO_INCREMENT,
    asset_id    BIGINT       NOT NULL,
    attr_key    VARCHAR(50)  NOT NULL,
    attr_value  VARCHAR(500) NOT NULL,
    PRIMARY KEY (attr_id),
    UNIQUE KEY uk_asset_attr (asset_id, attr_key),
    CONSTRAINT fk_attr_asset FOREIGN KEY (asset_id)
        REFERENCES asset (asset_id) ON DELETE CASCADE,
    CONSTRAINT fk_attr_def FOREIGN KEY (attr_key)
        REFERENCES attribute_def (attr_key)
) ENGINE = InnoDB COMMENT = '자산 확장 속성';

CREATE TABLE dependency (
    dependency_id    BIGINT       NOT NULL AUTO_INCREMENT,
    project_id       BIGINT       NOT NULL,
    from_asset_id    BIGINT       NOT NULL,
    to_asset_id      BIGINT                COMMENT '대상이 등록 자산이 아니면 NULL',
    target_address   VARCHAR(255) NOT NULL COMMENT '설정에 적힌 접속 주소 (IP 또는 도메인)',
    target_port      INT          NOT NULL,
    protocol         VARCHAR(20)  NOT NULL COMMENT 'JDBC / HTTP / HTTPS / TCP 등',
    config_location  VARCHAR(255)          COMMENT '예: application.yml spring.datasource.url',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (dependency_id),
    KEY idx_dep_target (target_address),
    CONSTRAINT fk_dep_project FOREIGN KEY (project_id)
        REFERENCES migration_project (project_id) ON DELETE CASCADE,
    CONSTRAINT fk_dep_from FOREIGN KEY (from_asset_id)
        REFERENCES asset (asset_id) ON DELETE CASCADE,
    CONSTRAINT fk_dep_to FOREIGN KEY (to_asset_id)
        REFERENCES asset (asset_id) ON DELETE SET NULL,
    CONSTRAINT chk_dep_port CHECK (target_port BETWEEN 1 AND 65535)
) ENGINE = InnoDB COMMENT = '자산 간 연결';

CREATE TABLE dns_record (
    dns_id       BIGINT       NOT NULL AUTO_INCREMENT,
    project_id   BIGINT       NOT NULL,
    domain       VARCHAR(255) NOT NULL,
    record_type  VARCHAR(10)  NOT NULL COMMENT 'A / AAAA / CNAME / MX / TXT',
    value        VARCHAR(255) NOT NULL,
    ttl          INT          NOT NULL COMMENT '초 단위',
    phase        VARCHAR(10)  NOT NULL COMMENT 'BEFORE / AFTER',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (dns_id),
    KEY idx_dns_value (value, phase),
    CONSTRAINT fk_dns_project FOREIGN KEY (project_id)
        REFERENCES migration_project (project_id) ON DELETE CASCADE,
    CONSTRAINT chk_dns_phase CHECK (phase IN ('BEFORE', 'AFTER')),
    CONSTRAINT chk_dns_ttl CHECK (ttl >= 0)
) ENGINE = InnoDB COMMENT = 'DNS 레코드 (이전 전/후)';

CREATE TABLE risk_factor (
    factor_id    BIGINT        NOT NULL AUTO_INCREMENT,
    code         VARCHAR(30)   NOT NULL,
    name         VARCHAR(100)  NOT NULL,
    weight       DECIMAL(4, 3) NOT NULL,
    description  VARCHAR(255),
    PRIMARY KEY (factor_id),
    UNIQUE KEY uk_factor_code (code),
    CONSTRAINT chk_factor_weight CHECK (weight > 0 AND weight <= 1)
) ENGINE = InnoDB COMMENT = '위험 요인 (가중치)';

CREATE TABLE risk_rule (
    rule_id           BIGINT       NOT NULL AUTO_INCREMENT,
    rule_code         VARCHAR(20)  NOT NULL,
    factor_id         BIGINT       NOT NULL,
    title             VARCHAR(150) NOT NULL,
    severity          TINYINT      NOT NULL COMMENT '심각도 1~10',
    occurrence        TINYINT      NOT NULL COMMENT '발생가능성 1~10',
    detection         TINYINT      NOT NULL COMMENT '검출 난이도 1~10 (높을수록 발견 어려움)',
    is_blocking       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1이면 점수와 무관하게 전환 차단',
    message_template  TEXT         NOT NULL COMMENT '{asset} {target} {address} {port} {config} {domain} {ttl} 치환',
    mitigation        TEXT         NOT NULL,
    enabled           TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (rule_id),
    UNIQUE KEY uk_rule_code (rule_code),
    CONSTRAINT fk_rule_factor FOREIGN KEY (factor_id)
        REFERENCES risk_factor (factor_id),
    CONSTRAINT chk_rule_s CHECK (severity   BETWEEN 1 AND 10),
    CONSTRAINT chk_rule_o CHECK (occurrence BETWEEN 1 AND 10),
    CONSTRAINT chk_rule_d CHECK (detection  BETWEEN 1 AND 10)
) ENGINE = InnoDB COMMENT = '진단 규칙';

CREATE TABLE diagnosis_run (
    run_id         BIGINT        NOT NULL AUTO_INCREMENT,
    project_id     BIGINT        NOT NULL,
    executed_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_score    DECIMAL(5, 2) NOT NULL DEFAULT 0 COMMENT '0~100',
    risk_level     VARCHAR(10)   NOT NULL COMMENT 'LOW / MEDIUM / HIGH',
    blocked        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '차단 규칙 1건 이상 발견 시 1',
    finding_count  INT           NOT NULL DEFAULT 0,
    ai_summary     TEXT                   COMMENT 'AI 요약 (선택)',
    PRIMARY KEY (run_id),
    KEY idx_run_project (project_id, executed_at),
    CONSTRAINT fk_run_project FOREIGN KEY (project_id)
        REFERENCES migration_project (project_id) ON DELETE CASCADE,
    CONSTRAINT chk_run_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH'))
) ENGINE = InnoDB COMMENT = '진단 실행 이력';

CREATE TABLE diagnosis_finding (
    finding_id     BIGINT   NOT NULL AUTO_INCREMENT,
    run_id         BIGINT   NOT NULL,
    rule_id        BIGINT   NOT NULL,
    asset_id       BIGINT,
    dependency_id  BIGINT,
    dns_id         BIGINT,
    rpn            SMALLINT NOT NULL,
    detail         TEXT     NOT NULL COMMENT '치환이 끝난 피드백 문구',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (finding_id),
    KEY idx_finding_run (run_id),
    CONSTRAINT fk_finding_run FOREIGN KEY (run_id)
        REFERENCES diagnosis_run (run_id) ON DELETE CASCADE,
    CONSTRAINT fk_finding_rule FOREIGN KEY (rule_id)
        REFERENCES risk_rule (rule_id),
    CONSTRAINT fk_finding_asset FOREIGN KEY (asset_id)
        REFERENCES asset (asset_id) ON DELETE SET NULL,
    CONSTRAINT fk_finding_dep FOREIGN KEY (dependency_id)
        REFERENCES dependency (dependency_id) ON DELETE SET NULL,
    CONSTRAINT fk_finding_dns FOREIGN KEY (dns_id)
        REFERENCES dns_record (dns_id) ON DELETE SET NULL
) ENGINE = InnoDB COMMENT = '진단 결과 항목';
