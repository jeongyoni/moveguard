package com.moveguard.imports;

import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

/**
 * 입력 저장용 INSERT. 생성 키는 파라미터 Map에 담겨 돌아온다(projectId, assetId).
 * 자산은 이름→id 매핑을 서비스가 만들어 각 하위 행 저장에 쓴다.
 */
@Mapper
public interface ProjectImportMapper {

    void insertProject(Map<String, Object> params);

    void insertAsset(Map<String, Object> params);

    void insertIp(Map<String, Object> params);

    void insertDependency(Map<String, Object> params);

    void insertDns(Map<String, Object> params);

    void insertCertificate(Map<String, Object> params);

    void insertSoftware(Map<String, Object> params);

    void insertBackup(Map<String, Object> params);
}
