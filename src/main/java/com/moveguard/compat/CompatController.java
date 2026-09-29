package com.moveguard.compat;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compat")
@RequiredArgsConstructor
public class CompatController {

    private final CompatSyncService compatSyncService;

    /** 호환성 기준 데이터를 endoflife.date(실패 시 스냅샷)와 동기화한다. */
    @PostMapping("/sync")
    public List<CompatSyncService.SyncResult> sync() {
        return compatSyncService.sync();
    }
}
