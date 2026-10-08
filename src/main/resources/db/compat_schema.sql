-- =====================================================================
-- 호환성 진단용 테이블
-- 기준 데이터(compat_*)는 endoflife.date에서 주기 동기화해 보관한다.
-- 규칙은 외부 API를 직접 호출하지 않고 이 테이블만 참조한다.
-- =====================================================================
SET NAMES utf8mb4;

DROP TABLE IF EXISTS asset_software;
DROP TABLE IF EXISTS compat_release;
DROP TABLE IF EXISTS compat_product;

-- 호환성 기준 제품 (동기화 대상)
CREATE TABLE compat_product (
    product          VARCHAR(50)  NOT NULL COMMENT 'endoflife.date 제품 키 (tomcat, java, mysql)',
    label            VARCHAR(100) NOT NULL,
    category         VARCHAR(30)           COMMENT 'database / server-app / lang / os',
    version_command  VARCHAR(200)          COMMENT '현장에서 버전 확인용 명령',
    source_url       VARCHAR(255),
    fetched_at       DATETIME              COMMENT '마지막 동기화 시각',
    fetch_status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (product),
    CONSTRAINT chk_fetch_status CHECK (fetch_status IN ('PENDING', 'OK', 'FAILED', 'MANUAL'))
) ENGINE = InnoDB COMMENT = '호환성 기준 제품';

-- 릴리스별 지원 정보
CREATE TABLE compat_release (
    release_id        BIGINT      NOT NULL AUTO_INCREMENT,
    product           VARCHAR(50) NOT NULL,
    version           VARCHAR(30) NOT NULL COMMENT '릴리스 라인 (9.0, 19, 8.4)',
    label             VARCHAR(50),
    release_date      DATE,
    is_lts            TINYINT(1)  NOT NULL DEFAULT 0,
    is_eol            TINYINT(1)  NOT NULL DEFAULT 0,
    eol_date          DATE                 COMMENT '지원 종료일',
    ext_support_date  DATE                 COMMENT '연장 지원 종료일 (Oracle 등)',
    is_maintained     TINYINT(1)  NOT NULL DEFAULT 1,
    latest_version    VARCHAR(30),
    min_java_version  VARCHAR(10)          COMMENT '일부 제품만 제공 (Tomcat 등)',
    is_manual         TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '1이면 동기화가 덮어쓰지 않음',
    updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (release_id),
    UNIQUE KEY uk_compat_release (product, version),
    KEY idx_compat_eol (product, is_eol),
    CONSTRAINT fk_release_product FOREIGN KEY (product)
        REFERENCES compat_product (product) ON DELETE CASCADE
) ENGINE = InnoDB COMMENT = '제품 릴리스별 지원 정보';

-- JDBC 드라이버↔DB 요구 버전 (endoflife.date에 없어 수동 관리)
CREATE TABLE driver_requirement (
    requirement_id   BIGINT      NOT NULL AUTO_INCREMENT,
    db_product       VARCHAR(50) NOT NULL COMMENT '대상 DB 제품 (mysql, oracle-database 등)',
    db_release_line  VARCHAR(30)          COMMENT 'NULL이면 제품 전체, 아니면 특정 라인(8.4 등)',
    driver_product   VARCHAR(60) NOT NULL COMMENT '드라이버 제품명 (mysql-connector-j, ojdbc8 등)',
    min_version      VARCHAR(30) NOT NULL COMMENT '요구되는 최소 드라이버 버전',
    note             VARCHAR(255),
    is_manual        TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '수동 관리 데이터',
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (requirement_id),
    KEY idx_driver_req (db_product, driver_product)
) ENGINE = InnoDB COMMENT = 'JDBC 드라이버 호환 매트릭스 (수동 관리)';

-- 자산에 설치된 소프트웨어 (이전 전/후)
CREATE TABLE asset_software (
    software_id     BIGINT      NOT NULL AUTO_INCREMENT,
    asset_id        BIGINT      NOT NULL,
    product         VARCHAR(50) NOT NULL COMMENT 'compat_product.product와 같은 키',
    role            VARCHAR(20)          COMMENT 'WAS / DB / RUNTIME / WEB',
    version         VARCHAR(30) NOT NULL COMMENT '실제 버전 (9.0.80)',
    release_line    VARCHAR(20) NOT NULL COMMENT 'compat_release.version과 매칭 (9.0)',
    phase           VARCHAR(10) NOT NULL COMMENT 'BEFORE / AFTER',
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (software_id),
    UNIQUE KEY uk_asset_software (asset_id, product, phase),
    KEY idx_software_lookup (product, release_line),
    CONSTRAINT fk_software_asset FOREIGN KEY (asset_id)
        REFERENCES asset (asset_id) ON DELETE CASCADE,
    CONSTRAINT chk_software_phase CHECK (phase IN ('BEFORE', 'AFTER'))
) ENGINE = InnoDB COMMENT = '자산별 설치 소프트웨어 (이전 전/후)';
