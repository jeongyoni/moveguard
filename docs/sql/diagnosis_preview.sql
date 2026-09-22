-- 진단 규칙 SQL 미리보기 (project_id = 1)
-- Java 규칙 엔진 구현 전, 더미 데이터에서 규칙이 의도대로 걸리는지 확인
SET @p := 1;

WITH changing_public_ip AS (
    SELECT b.asset_id, b.address, b.ext_whitelisted
    FROM asset_ip b
    JOIN asset a ON a.asset_id = b.asset_id
    WHERE a.project_id = @p
      AND b.phase = 'BEFORE'
      AND b.ip_type = 'PUBLIC'
      AND NOT EXISTS (
          SELECT 1 FROM asset_ip x
          WHERE x.asset_id = b.asset_id
            AND x.phase = 'AFTER'
            AND x.address = b.address
      )
)
SELECT 'IP-01' AS rule_code, fa.name AS asset, ta.name AS target,
       d.target_address AS address, d.target_port AS port, d.config_location
FROM dependency d
JOIN asset fa ON fa.asset_id = d.from_asset_id
JOIN asset ta ON ta.asset_id = d.to_asset_id
JOIN changing_public_ip c ON c.asset_id = d.to_asset_id AND c.address = d.target_address
WHERE d.project_id = @p
UNION ALL
SELECT 'IP-02', fa.name, ta.name, d.target_address, d.target_port, d.config_location
FROM dependency d
JOIN asset fa ON fa.asset_id = d.from_asset_id
JOIN asset ta ON ta.asset_id = d.to_asset_id AND ta.asset_type = 'SERVER'
JOIN asset_ip i ON i.asset_id = ta.asset_id
               AND i.address = d.target_address
               AND i.ip_type = 'PUBLIC'
               AND i.phase = 'BEFORE'
WHERE d.project_id = @p
UNION ALL
SELECT 'IP-03', fa.name, NULL, d.target_address, d.target_port, d.config_location
FROM dependency d
JOIN asset fa ON fa.asset_id = d.from_asset_id
WHERE d.project_id = @p
  AND d.target_address REGEXP '^[0-9]{1,3}(\\.[0-9]{1,3}){3}$'
UNION ALL
SELECT 'IP-04', a.name, NULL, c.address, NULL, NULL
FROM changing_public_ip c
JOIN asset a ON a.asset_id = c.asset_id
WHERE c.ext_whitelisted = 1
UNION ALL
SELECT 'DNS-01', r.domain, NULL, r.value, r.ttl, NULL
FROM dns_record r
JOIN changing_public_ip c ON c.address = r.value
WHERE r.project_id = @p
  AND r.phase = 'BEFORE'
  AND r.record_type = 'A'
  AND r.ttl > 300
UNION ALL
SELECT 'DNS-02', r.domain, NULL, r.value, r.ttl, NULL
FROM dns_record r
JOIN changing_public_ip c ON c.address = r.value
WHERE r.project_id = @p
  AND r.phase = 'BEFORE'
  AND r.record_type = 'A'
  AND NOT EXISTS (
      SELECT 1 FROM dns_record n
      WHERE n.project_id = r.project_id
        AND n.domain = r.domain
        AND n.record_type = r.record_type
        AND n.phase = 'AFTER'
  )
ORDER BY rule_code;
