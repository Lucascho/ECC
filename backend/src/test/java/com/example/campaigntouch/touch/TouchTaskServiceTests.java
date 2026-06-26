package com.example.campaigntouch.touch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.campaigntouch.campaign.Campaign;
import com.example.campaigntouch.campaign.CampaignRepository;
import com.example.campaigntouch.campaign.CampaignStatus;
import com.example.campaigntouch.campaign.CampaignType;
import com.example.campaigntouch.common.BusinessException;
import com.example.campaigntouch.common.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TouchTaskServiceTests {

    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 6, 18, 0, 0);
    private static final LocalDateTime END_TIME = LocalDateTime.of(2026, 6, 18, 23, 59);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CampaignRepositoryStub campaignRepository;
    private TouchTaskRepositoryStub touchTaskRepository;
    private TouchTaskService touchTaskService;

    @BeforeEach
    void setUp() {
        campaignRepository = new CampaignRepositoryStub();
        touchTaskRepository = new TouchTaskRepositoryStub();
        touchTaskService = new TouchTaskService(touchTaskRepository.repository(), campaignRepository.repository());
    }

    @Test
    void createTouchTaskDefaultsToPendingForDraftCampaign() {
        Campaign campaign = campaign(CampaignStatus.DRAFT);
        campaignRepository.seed(1L, campaign);

        TouchTaskResponse response = touchTaskService.create(1L, validRequest());

        assertEquals(1L, response.campaignId());
        assertEquals("618 3C 首波觸達", response.taskName());
        assertEquals(TouchTaskStatus.PENDING, response.status());
        assertEquals(List.of(TouchChannel.IN_APP, TouchChannel.PUSH), response.channels());
        assertEquals(List.of("VIP", "GOLD"), response.audienceRule().memberLevels());
        assertEquals(30, response.audienceRule().lastLoginDaysLessThan());
        assertEquals(List.of("3C"), response.audienceRule().favoriteCategories());
        assertEquals(true, response.audienceRule().hasCartItems());
        assertNull(response.executedAt());
    }

    @Test
    void createTouchTaskAllowsActiveCampaign() {
        campaignRepository.seed(1L, campaign(CampaignStatus.ACTIVE));

        TouchTaskResponse response = touchTaskService.create(1L, validRequest());

        assertEquals(TouchTaskStatus.PENDING, response.status());
    }

    @Test
    void createTouchTaskRejectsPausedCampaign() {
        campaignRepository.seed(1L, campaign(CampaignStatus.PAUSED));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> touchTaskService.create(1L, validRequest()));

        assertEquals(ErrorCode.INVALID_CAMPAIGN_STATUS, exception.getErrorCode());
    }

    @Test
    void createTouchTaskRejectsMissingCampaign() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> touchTaskService.create(404L, validRequest()));

        assertEquals(ErrorCode.CAMPAIGN_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void createTouchTaskRejectsUnsupportedAudienceRuleField() {
        campaignRepository.seed(1L, campaign(CampaignStatus.DRAFT));
        TouchTaskRequest request = requestWithAudienceRule(json("""
                {
                  "memberLevels": ["VIP"],
                  "unsupported": true
                }
                """));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> touchTaskService.create(1L, request));

        assertEquals(ErrorCode.INVALID_AUDIENCE_RULE, exception.getErrorCode());
    }

    @Test
    void createTouchTaskRejectsInvalidAudienceRuleType() {
        campaignRepository.seed(1L, campaign(CampaignStatus.DRAFT));
        TouchTaskRequest request = requestWithAudienceRule(json("""
                {
                  "lastLoginDaysLessThan": -1
                }
                """));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> touchTaskService.create(1L, request));

        assertEquals(ErrorCode.INVALID_AUDIENCE_RULE, exception.getErrorCode());
    }

    @Test
    void createTouchTaskRejectsUnsupportedChannel() {
        campaignRepository.seed(1L, campaign(CampaignStatus.DRAFT));
        TouchTaskRequest request = requestWithChannels(List.of("IN_APP", "SMS"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> touchTaskService.create(1L, request));

        assertEquals(ErrorCode.UNSUPPORTED_TOUCH_CHANNEL, exception.getErrorCode());
    }

    @Test
    void createTouchTaskRejectsDuplicatedChannels() {
        campaignRepository.seed(1L, campaign(CampaignStatus.DRAFT));
        TouchTaskRequest request = requestWithChannels(List.of("IN_APP", "IN_APP"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> touchTaskService.create(1L, request));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void listByCampaignRequiresExistingCampaign() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> touchTaskService.listByCampaign(404L));

        assertEquals(ErrorCode.CAMPAIGN_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void listByCampaignReturnsTasksForCampaign() {
        Campaign campaign = campaign(CampaignStatus.DRAFT);
        campaignRepository.seed(1L, campaign);
        TouchTask touchTask = touchTask(campaign);
        touchTaskRepository.seedByCampaign(1L, List.of(touchTask));

        List<TouchTaskListResponse> responses = touchTaskService.listByCampaign(1L);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).campaignId());
        assertEquals(List.of(TouchChannel.IN_APP), responses.get(0).channels());
    }

    @Test
    void getMissingTouchTaskThrowsTouchTaskNotFound() {
        BusinessException exception = assertThrows(BusinessException.class, () -> touchTaskService.get(404L));

        assertEquals(ErrorCode.TOUCH_TASK_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getTouchTaskReturnsDetail() {
        Campaign campaign = campaign(CampaignStatus.DRAFT);
        TouchTask touchTask = touchTask(campaign);
        touchTaskRepository.seed(10L, touchTask);

        TouchTaskResponse response = touchTaskService.get(10L);

        assertEquals(1L, response.campaignId());
        assertEquals("task", response.taskName());
        assertEquals("title", response.messageTitle());
        assertEquals("content", response.messageContent());
    }

    private TouchTaskRequest validRequest() {
        return new TouchTaskRequest(
                "618 3C 首波觸達",
                json("""
                        {
                          "memberLevels": ["VIP", "GOLD"],
                          "lastLoginDaysLessThan": 30,
                          "favoriteCategories": ["3C"],
                          "hasCartItems": true
                        }
                        """),
                List.of("IN_APP", "PUSH"),
                "618 3C 限時優惠",
                "今晚 12 點前，指定 3C 商品限時優惠。"
        );
    }

    private TouchTaskRequest requestWithAudienceRule(JsonNode audienceRule) {
        TouchTaskRequest validRequest = validRequest();
        return new TouchTaskRequest(
                validRequest.taskName(),
                audienceRule,
                validRequest.channels(),
                validRequest.messageTitle(),
                validRequest.messageContent()
        );
    }

    private TouchTaskRequest requestWithChannels(List<String> channels) {
        TouchTaskRequest validRequest = validRequest();
        return new TouchTaskRequest(
                validRequest.taskName(),
                validRequest.audienceRule(),
                channels,
                validRequest.messageTitle(),
                validRequest.messageContent()
        );
    }

    private JsonNode json(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    private Campaign campaign(CampaignStatus status) {
        Campaign campaign = Campaign.draft(
                "campaign",
                CampaignType.PROMOTION,
                "description",
                "https://www.example.com/campaigns/618",
                START_TIME,
                END_TIME,
                "admin"
        );
        campaign.changeStatus(status);
        setId(campaign, 1L);
        return campaign;
    }

    private TouchTask touchTask(Campaign campaign) {
        TouchTask touchTask = TouchTask.pending(
                campaign,
                "task",
                new AudienceRule(null, null, null, null),
                List.of(TouchChannel.IN_APP),
                "title",
                "content"
        );
        setId(touchTask, 10L);
        return touchTask;
    }

    private void setId(Object target, Long id) {
        try {
            Field idField = target.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class CampaignRepositoryStub implements InvocationHandler {

        private final Map<Long, Campaign> campaigns = new HashMap<>();

        CampaignRepository repository() {
            return (CampaignRepository) Proxy.newProxyInstance(
                    CampaignRepository.class.getClassLoader(),
                    new Class<?>[] { CampaignRepository.class },
                    this
            );
        }

        void seed(Long id, Campaign campaign) {
            campaigns.put(id, campaign);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findById" -> Optional.ofNullable(campaigns.get(args[0]));
                case "toString" -> "CampaignRepositoryStub";
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private static final class TouchTaskRepositoryStub implements InvocationHandler {

        private final Map<Long, TouchTask> tasks = new HashMap<>();
        private final Map<Long, List<TouchTask>> tasksByCampaign = new HashMap<>();

        TouchTaskRepository repository() {
            return (TouchTaskRepository) Proxy.newProxyInstance(
                    TouchTaskRepository.class.getClassLoader(),
                    new Class<?>[] { TouchTaskRepository.class },
                    this
            );
        }

        void seed(Long id, TouchTask touchTask) {
            tasks.put(id, touchTask);
        }

        void seedByCampaign(Long campaignId, List<TouchTask> touchTasks) {
            tasksByCampaign.put(campaignId, new ArrayList<>(touchTasks));
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "save" -> args[0];
                case "findById" -> Optional.ofNullable(tasks.get(args[0]));
                case "findByCampaignId" -> tasksByCampaign.getOrDefault(args[0], List.of());
                case "toString" -> "TouchTaskRepositoryStub";
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == long.class || returnType == int.class || returnType == short.class
                || returnType == byte.class) {
            return 0;
        }
        if (returnType == float.class || returnType == double.class) {
            return 0.0;
        }
        return null;
    }
}
