package com.moveguard.compat;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CompatMapper {

    List<CompatProduct> findProducts();

    /** 수동 관리(is_manual = 1) 릴리스의 version 목록 — 동기화가 덮어쓰지 않도록 제외용 */
    List<String> findManualVersions(String product);

    void upsertRelease(CompatRelease release);

    void updateProductStatus(@Param("product") String product,
                             @Param("status") String status,
                             @Param("fetchedAt") LocalDateTime fetchedAt);
}
