package com.moveguard.project;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

/** 상세 화면에서의 항목 편집(CRUD). 2b: 자산. 이후 슬라이스에서 IP·DNS 등 확장. */
@Mapper
public interface ProjectEditMapper {

    List<AssetDetail> findAssets(Long projectId);

    AssetDetail findAsset(Long assetId);

    List<String> findAssetNames(Long projectId);

    void updateAsset(Map<String, Object> params);

    void deleteAsset(Long assetId);
}
