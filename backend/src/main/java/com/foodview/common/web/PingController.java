package com.foodview.common.web;

import com.foodview.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 连通性检查，前端用它确认代理和统一响应体都对得上。
 */
@RestController
public class PingController {

    @GetMapping("/ping")
    public ApiResponse<PingResponse> ping() {
        return ApiResponse.ok(new PingResponse("up"));
    }

    public record PingResponse(String status) {
    }
}
