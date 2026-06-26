package com.example.campaigntouch.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.campaigntouch.common.BusinessException;
import com.example.campaigntouch.common.ErrorCode;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class CampaignServiceTests {

    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 6, 18, 0, 0);
    private static final LocalDateTime END_TIME = LocalDateTime.of(2026, 6, 18, 23, 59);

    private CampaignRepositoryStub campaignRepository;
    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        campaignRepository = new CampaignRepositoryStub();
        campaignService = new CampaignService(campaignRepository.repository());
    }

    @Test
    void createCampaignDefaultsToDraftAndUsesAdminUser() {
        CampaignResponse response = campaignService.create(validRequest("618 3C"), "admin");

        assertEquals("618 3C", response.name());
        assertEquals(CampaignStatus.DRAFT, response.status());
        assertEquals("admin", response.createdBy());
    }

    @Test
    void createCampaignRejectsEndTimeBeforeStartTime() {
        CampaignRequest request = new CampaignRequest(
                "bad range",
                CampaignType.PROMOTION,
                null,
                null,
                END_TIME,
                START_TIME
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> campaignService.create(request, "admin"));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void updateDraftCampaignAllowsAllEditableFields() {
        Campaign campaign = draftCampaign();
        campaignRepository.seed(1L, campaign);

        CampaignRequest request = new CampaignRequest(
                "updated",
                CampaignType.NEW_PRODUCT,
                "updated description",
                "https://www.example.com/new",
                START_TIME.plusDays(1),
                END_TIME.plusDays(1)
        );

        CampaignResponse response = campaignService.update(1L, request);

        assertEquals("updated", response.name());
        assertEquals(CampaignType.NEW_PRODUCT, response.type());
        assertEquals("updated description", response.description());
        assertEquals("https://www.example.com/new", response.landingPageUrl());
        assertEquals(START_TIME.plusDays(1), response.startTime());
        assertEquals(END_TIME.plusDays(1), response.endTime());
    }

    @Test
    void updateActiveCampaignOnlyChangesDescriptionAndLandingPageUrl() {
        Campaign campaign = draftCampaign();
        campaign.changeStatus(CampaignStatus.ACTIVE);
        campaignRepository.seed(1L, campaign);

        CampaignRequest request = new CampaignRequest(
                "name should be ignored",
                CampaignType.NEW_PRODUCT,
                "active description",
                "https://www.example.com/active",
                START_TIME.plusDays(1),
                END_TIME.plusDays(1)
        );

        CampaignResponse response = campaignService.update(1L, request);

        assertEquals("original", response.name());
        assertEquals(CampaignType.PROMOTION, response.type());
        assertEquals("active description", response.description());
        assertEquals("https://www.example.com/active", response.landingPageUrl());
        assertEquals(START_TIME, response.startTime());
        assertEquals(END_TIME, response.endTime());
    }

    @Test
    void updatePausedCampaignIsRejected() {
        Campaign campaign = draftCampaign();
        campaign.changeStatus(CampaignStatus.PAUSED);
        campaignRepository.seed(1L, campaign);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> campaignService.update(1L, validRequest("updated")));

        assertEquals(ErrorCode.INVALID_CAMPAIGN_STATUS, exception.getErrorCode());
    }

    @Test
    void activateAllowsDraftAndPausedCampaigns() {
        Campaign campaign = draftCampaign();
        campaignRepository.seed(1L, campaign);

        CampaignStatusResponse response = campaignService.activate(1L);

        assertEquals(CampaignStatus.ACTIVE, response.status());
    }

    @Test
    void activateRejectsEndedCampaign() {
        Campaign campaign = draftCampaign();
        campaign.changeStatus(CampaignStatus.ENDED);
        campaignRepository.seed(1L, campaign);

        BusinessException exception = assertThrows(BusinessException.class, () -> campaignService.activate(1L));

        assertEquals(ErrorCode.INVALID_CAMPAIGN_STATUS, exception.getErrorCode());
    }

    @Test
    void pauseOnlyAllowsActiveCampaign() {
        Campaign campaign = draftCampaign();
        campaign.changeStatus(CampaignStatus.ACTIVE);
        campaignRepository.seed(1L, campaign);

        CampaignStatusResponse response = campaignService.pause(1L);

        assertEquals(CampaignStatus.PAUSED, response.status());
    }

    @Test
    void endAllowsDraftActiveAndPausedCampaigns() {
        Campaign campaign = draftCampaign();
        campaign.changeStatus(CampaignStatus.PAUSED);
        campaignRepository.seed(1L, campaign);

        CampaignStatusResponse response = campaignService.end(1L);

        assertEquals(CampaignStatus.ENDED, response.status());
    }

    @Test
    void getMissingCampaignThrowsCampaignNotFound() {
        BusinessException exception = assertThrows(BusinessException.class, () -> campaignService.get(404L));

        assertEquals(ErrorCode.CAMPAIGN_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void listDelegatesStatusAndTypeFilterToRepository() {
        campaignService.list(CampaignStatus.ACTIVE, CampaignType.PROMOTION);

        assertEquals("findByStatusAndType", campaignRepository.lastMethodName());
        assertEquals(CampaignStatus.ACTIVE, campaignRepository.lastArguments()[0]);
        assertEquals(CampaignType.PROMOTION, campaignRepository.lastArguments()[1]);
    }

    private CampaignRequest validRequest(String name) {
        return new CampaignRequest(
                name,
                CampaignType.PROMOTION,
                "description",
                "https://www.example.com/campaigns/618",
                START_TIME,
                END_TIME
        );
    }

    private Campaign draftCampaign() {
        return Campaign.draft(
                "original",
                CampaignType.PROMOTION,
                "description",
                "https://www.example.com/campaigns/618",
                START_TIME,
                END_TIME,
                "admin"
        );
    }

    private static final class CampaignRepositoryStub implements InvocationHandler {

        private final Map<Long, Campaign> campaigns = new HashMap<>();
        private String lastMethodName;
        private Object[] lastArguments = new Object[0];

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

        String lastMethodName() {
            return lastMethodName;
        }

        Object[] lastArguments() {
            return lastArguments;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            lastMethodName = method.getName();
            lastArguments = args == null ? new Object[0] : args;

            return switch (method.getName()) {
                case "save" -> args[0];
                case "findById" -> Optional.ofNullable(campaigns.get(args[0]));
                case "findAll", "findByStatus", "findByType", "findByStatusAndType" -> List.of();
                case "toString" -> "CampaignRepositoryStub";
                default -> defaultValue(method.getReturnType());
            };
        }

        private Object defaultValue(Class<?> returnType) {
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
            if (returnType == void.class) {
                return null;
            }
            if (returnType == Sort.class) {
                return Sort.unsorted();
            }
            return null;
        }
    }
}
