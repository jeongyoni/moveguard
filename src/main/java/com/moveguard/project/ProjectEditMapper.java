package com.moveguard.project;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 상세 화면에서의 항목 편집(CRUD). 2b: 자산, 2c: IP. 이후 슬라이스에서 DNS 등 확장. */
@Mapper
public interface ProjectEditMapper {

    List<AssetDetail> findAssets(Long projectId);

    AssetDetail findAsset(Long assetId);

    List<String> findAssetNames(Long projectId);

    void updateAsset(Map<String, Object> params);

    void deleteAsset(Long assetId);

    // IP
    List<IpView> findIps(Long projectId);

    IpView findIp(Long ipId);

    /** (자산·주소·단계) 중복 확인 — 있으면 해당 ip_id, 없으면 null */
    Long findIpId(@Param("assetId") Long assetId, @Param("address") String address,
                  @Param("phase") String phase);

    void updateIp(Map<String, Object> params);

    void deleteIp(Long ipId);
}
