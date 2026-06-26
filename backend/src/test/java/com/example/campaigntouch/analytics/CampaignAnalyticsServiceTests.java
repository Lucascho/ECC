package com.example.campaigntouch.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.campaigntouch.campaign.Campaign;
import com.example.campaigntouch.campaign.CampaignRepository;
import com.example.campaigntouch.campaign.CampaignType;
import com.example.campaigntouch.common.BusinessException;
import com.example.campaigntouch.common.ErrorCode;
import com.example.campaigntouch.touch.TouchChannel;
import com.example.campaigntouch.touch.TouchDeliveryRepository;
import com.example.campaigntouch.touch.TouchDeliveryStatus;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CampaignAnalyticsServiceTests {

    private CampaignRepositoryStub campaignRepository;
    private TouchDeliveryRepositoryStub touchDeliveryRepository;
    private CampaignAnalyticsService campaignAnalyticsService;

    @BeforeEach
    void setUp() {
        campaignRepository = new CampaignRepositoryStub();
        touchDeliveryRepository = new TouchDeliveryRepositoryStub();
        campaignAnalyticsService = new CampaignAnalyticsService(
                campaignRepository.repository(),
                touchDeliveryRepository.repository()
        );
    }

    @Test
    void getAnalyticsUsesTouchDeliveryAsSourceOfTruth() {
        campaignRepository.seed(campaign(1L, "618 3C"));
        touchDeliveryRepository.targetMemberCount = 3;
        touchDeliveryRepository.deliveryCount = 5;
        touchDeliveryRepository.sentCount = 4;
        touchDeliveryRepository.failedCount = 1;
        touchDeliveryRepository.inAppSentCount = 2;
        touchDeliveryRepository.clickCount = 1;

        CampaignAnalyticsResponse response = campaignAnalyticsService.getAnalytics(1L);

        assertEquals(1L, response.campaignId());
        assertEquals("618 3C", response.campaignName());
        assertEquals(3, response.targetMemberCount());
        assertEquals(5, response.deliveryCount());
        assertEquals(4, response.sentCount());
        assertEquals(1, response.failedCount());
        assertEquals(2, response.inAppSentCount());
        assertEquals(1, response.clickCount());
        assertEquals(0.5, response.clickThroughRate());
        assertEquals(0, touchDeliveryRepository.campaignEventQueryCount);
    }

    @Test
    void clickThroughRateIsZeroWhenThereAreNoInAppSentDeliveries() {
        campaignRepository.seed(campaign(1L, "No In App"));
        touchDeliveryRepository.inAppSentCount = 0;
        touchDeliveryRepository.clickCount = 3;

        CampaignAnalyticsResponse response = campaignAnalyticsService.getAnalytics(1L);

        assertEquals(0.0, response.clickThroughRate());
    }

    @Test
    void missingCampaignThrowsCampaignNotFound() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> campaignAnalyticsService.getAnalytics(404L));

        assertEquals(ErrorCode.CAMPAIGN_NOT_FOUND, exception.getErrorCode());
    }

    private Campaign campaign(Long id, String name) {
        Campaign campaign = Campaign.draft(
                name,
                CampaignType.PROMOTION,
                "description",
                "https://www.example.com/campaigns/618",
                LocalDateTime.of(2026, 6, 18, 0, 0),
                LocalDateTime.of(2026, 6, 18, 23, 59),
                "admin"
        );
        setId(campaign, id);
        return campaign;
    }

    private static void setId(Object target, Long id) {
        try {
            Field idField = target.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
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

    private static final class CampaignRepositoryStub implements InvocationHandler {

        private Campaign campaign;

        CampaignRepository repository() {
            return (CampaignRepository) Proxy.newProxyInstance(
                    CampaignRepository.class.getClassLoader(),
                    new Class<?>[] { CampaignRepository.class },
                    this
            );
        }

        void seed(Campaign campaign) {
            this.campaign = campaign;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findById" -> Optional.ofNullable(campaign);
                case "toString" -> "CampaignRepositoryStub";
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private static final class TouchDeliveryRepositoryStub implements InvocationHandler {

        long targetMemberCount;
        long deliveryCount;
        long sentCount;
        long failedCount;
        long inAppSentCount;
        long clickCount;
        int campaignEventQueryCount;

        TouchDeliveryRepository repository() {
            return (TouchDeliveryRepository) Proxy.newProxyInstance(
                    TouchDeliveryRepository.class.getClassLoader(),
                    new Class<?>[] { TouchDeliveryRepository.class },
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "countDistinctMemberIdByCampaignId" -> targetMemberCount;
                case "countByCampaignId" -> deliveryCount;
                case "countByCampaignIdAndStatusIn" -> sentCount((Collection<?>) args[1]);
                case "countByCampaignIdAndStatus" -> countByStatus((TouchDeliveryStatus) args[1]);
                case "countByCampaignIdAndChannelAndStatusIn" -> inAppCountByStatuses(
                        (TouchChannel) args[1],
                        (Collection<?>) args[2]
                );
                case "countByCampaignIdAndChannelAndStatus" -> inAppCountByStatus(
                        (TouchChannel) args[1],
                        (TouchDeliveryStatus) args[2]
                );
                case "toString" -> "TouchDeliveryRepositoryStub";
                default -> defaultValue(method.getReturnType());
            };
        }

        private long sentCount(Collection<?> statuses) {
            if (statuses.contains(TouchDeliveryStatus.SENT) && statuses.contains(TouchDeliveryStatus.CLICKED)) {
                return sentCount;
            }
            return 0;
        }

        private long countByStatus(TouchDeliveryStatus status) {
            return status == TouchDeliveryStatus.FAILED ? failedCount : 0;
        }

        private long inAppCountByStatuses(TouchChannel channel, Collection<?> statuses) {
            if (channel == TouchChannel.IN_APP && statuses.contains(TouchDeliveryStatus.SENT)
                    && statuses.contains(TouchDeliveryStatus.CLICKED)) {
                return inAppSentCount;
            }
            return 0;
        }

        private long inAppCountByStatus(TouchChannel channel, TouchDeliveryStatus status) {
            return channel == TouchChannel.IN_APP && status == TouchDeliveryStatus.CLICKED ? clickCount : 0;
        }
    }
}
