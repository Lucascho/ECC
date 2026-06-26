package com.example.campaigntouch.touch;

import com.example.campaigntouch.common.BusinessException;
import com.example.campaigntouch.common.ErrorCode;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TouchTaskController {

    private final TouchTaskService touchTaskService;
    private final TouchExecutionService touchExecutionService;

    public TouchTaskController(TouchTaskService touchTaskService, TouchExecutionService touchExecutionService) {
        this.touchTaskService = touchTaskService;
        this.touchExecutionService = touchExecutionService;
    }

    @PostMapping("/api/admin/campaigns/{campaignId}/touch-tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TouchTaskResponse create(
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser,
            @PathVariable Long campaignId,
            @Valid @RequestBody TouchTaskRequest request
    ) {
        requireAdminUser(adminUser);
        return touchTaskService.create(campaignId, request);
    }

    @GetMapping("/api/admin/campaigns/{campaignId}/touch-tasks")
    public List<TouchTaskListResponse> listByCampaign(
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser,
            @PathVariable Long campaignId
    ) {
        requireAdminUser(adminUser);
        return touchTaskService.listByCampaign(campaignId);
    }

    @GetMapping("/api/admin/touch-tasks/{taskId}")
    public TouchTaskResponse get(
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser,
            @PathVariable Long taskId
    ) {
        requireAdminUser(adminUser);
        return touchTaskService.get(taskId);
    }

    @PostMapping("/api/admin/touch-tasks/{taskId}/execute")
    public TouchExecutionResponse execute(
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser,
            @PathVariable Long taskId
    ) {
        requireAdminUser(adminUser);
        return touchExecutionService.execute(taskId);
    }

    private void requireAdminUser(String adminUser) {
        if (adminUser == null || adminUser.isBlank()) {
            throw new BusinessException(ErrorCode.ADMIN_USER_REQUIRED);
        }
    }
}
