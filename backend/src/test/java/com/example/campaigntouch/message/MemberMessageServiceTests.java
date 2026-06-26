package com.example.campaigntouch.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.campaigntouch.analytics.CampaignEvent;
import com.example.campaigntouch.analytics.CampaignEventRepository;
import com.example.campaigntouch.analytics.CampaignEventType;
import com.example.campaigntouch.campaign.Campaign;
import com.example.campaigntouch.campaign.CampaignStatus;
import com.example.campaigntouch.campaign.CampaignType;
import com.example.campaigntouch.common.BusinessException;
import com.example.campaigntouch.common.ErrorCode;
import com.example.campaigntouch.touch.AudienceRule;
import com.example.campaigntouch.touch.DeliveryResult;
import com.example.campaigntouch.touch.TouchChannel;
import com.example.campaigntouch.touch.TouchDelivery;
import com.example.campaigntouch.touch.TouchDeliveryRepository;
import com.example.campaigntouch.touch.TouchDeliveryStatus;
import com.example.campaigntouch.touch.TouchTask;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemberMessageServiceTests {

    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 6, 18, 0, 0);
    private static final LocalDateTime END_TIME = LocalDateTime.of(2026, 6, 18, 23, 59);

    private MemberMessageRepositoryStub memberMessageRepository;
    private TouchDeliveryRepositoryStub touchDeliveryRepository;
    private CampaignEventRepositoryStub campaignEventRepository;
    private MemberMessageService memberMessageService;

    @BeforeEach
    void setUp() {
        memberMessageRepository = new MemberMessageRepositoryStub();
        touchDeliveryRepository = new TouchDeliveryRepositoryStub();
        campaignEventRepository = new CampaignEventRepositoryStub();
        memberMessageService = new MemberMessageService(
                memberMessageRepository.repository(),
                touchDeliveryRepository.repository(),
                campaignEventRepository.repository()
        );
    }

    @Test
    void listReturnsOnlyOwnMessages() {
        memberMessageRepository.seed(message(100L, "1001", CampaignStatus.ACTIVE));
        memberMessageRepository.seed(message(101L, "1002", CampaignStatus.ACTIVE));

        List<MemberMessageResponse> responses = memberMessageService.list("1001", null, false);

        assertEquals(1, responses.size());
        assertEquals(100L, responses.get(0).id());
    }

    @Test
    void listCanFilterUnreadMessages() {
        MemberMessage unreadMessage = message(100L, "1001", CampaignStatus.ACTIVE);
        MemberMessage readMessage = message(101L, "1001", CampaignStatus.ACTIVE);
        readMessage.click(LocalDateTime.now());
        memberMessageRepository.seed(unreadMessage);
        memberMessageRepository.seed(readMessage);

        List<MemberMessageResponse> responses = memberMessageService.list("1001", null, true);

        assertEquals(1, responses.size());
        assertEquals(100L, responses.get(0).id());
    }

    @Test
    void firstClickUpdatesMessageAndDeliveryAndCreatesClickEvent() {
        MemberMessage message = message(100L, "1001", CampaignStatus.ACTIVE);
        memberMessageRepository.seed(message);

        MemberMessageClickResponse response = memberMessageService.click(100L, "1001");

        assertNotNull(response.clickedAt());
        assertEquals(TouchDeliveryStatus.CLICKED, response.deliveryStatus());
        assertEquals(1, memberMessageRepository.saveCount());
        assertEquals(1, touchDeliveryRepository.saveCount());
        assertEquals(1, campaignEventRepository.saveCount());
        assertEquals(CampaignEventType.CLICK, campaignEventRepository.savedEvents().get(0).getEventType());
    }

    @Test
    void repeatedClickDoesNotCreateDuplicateClickEvent() {
        MemberMessage message = message(100L, "1001", CampaignStatus.ACTIVE);
        LocalDateTime clickedAt = LocalDateTime.of(2026, 6, 26, 11, 20);
        message.click(clickedAt);
        message.getDelivery().markClicked(clickedAt);
        memberMessageRepository.seed(message);

        MemberMessageClickResponse response = memberMessageService.click(100L, "1001");

        assertEquals(clickedAt, response.clickedAt());
        assertEquals(0, memberMessageRepository.saveCount());
        assertEquals(0, touchDeliveryRepository.saveCount());
        assertEquals(0, campaignEventRepository.saveCount());
    }

    @Test
    void memberCannotClickOtherMembersMessage() {
        memberMessageRepository.seed(message(100L, "1001", CampaignStatus.ACTIVE));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> memberMessageService.click(100L, "1002"));

        assertEquals(ErrorCode.MESSAGE_NOT_OWNED_BY_MEMBER, exception.getErrorCode());
    }

    @Test
    void onlyActiveCampaignCanRecordClick() {
        memberMessageRepository.seed(message(100L, "1001", CampaignStatus.PAUSED));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> memberMessageService.click(100L, "1001"));

        assertEquals(ErrorCode.INVALID_CAMPAIGN_STATUS, exception.getErrorCode());
    }

    @Test
    void missingMessageThrowsMessageNotFound() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> memberMessageService.click(404L, "1001"));

        assertEquals(ErrorCode.MESSAGE_NOT_FOUND, exception.getErrorCode());
    }

    private MemberMessage message(Long messageId, String memberId, CampaignStatus campaignStatus) {
        Campaign campaign = Campaign.draft(
                "campaign",
                CampaignType.PROMOTION,
                "description",
                "https://www.example.com/campaigns/618",
                START_TIME,
                END_TIME,
                "admin"
        );
        campaign.changeStatus(campaignStatus);
        setField(campaign, "id", 1L);
        TouchTask task = TouchTask.pending(
                campaign,
                "task",
                new AudienceRule(null, null, null, null),
                List.of(TouchChannel.IN_APP),
                "title",
                "content"
        );
        setField(task, "id", 10L);
        TouchDelivery delivery = TouchDelivery.pending(task, memberId, TouchChannel.IN_APP, "title", "content");
        setField(delivery, "id", messageId + 1000);
        delivery.applyResult(DeliveryResult.sent(), LocalDateTime.now());
        MemberMessage message = MemberMessage.fromDelivery(delivery);
        setField(message, "id", messageId);
        setField(message, "createdAt", LocalDateTime.now());
        return message;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
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

    private static final class MemberMessageRepositoryStub implements InvocationHandler {

        private final List<MemberMessage> messages = new ArrayList<>();
        private int saveCount;

        MemberMessageRepository repository() {
            return (MemberMessageRepository) Proxy.newProxyInstance(
                    MemberMessageRepository.class.getClassLoader(),
                    new Class<?>[] { MemberMessageRepository.class },
                    this
            );
        }

        void seed(MemberMessage message) {
            messages.add(message);
        }

        int saveCount() {
            return saveCount;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findById" -> messages.stream()
                        .filter(message -> message.getId().equals(args[0]))
                        .findFirst();
                case "save" -> {
                    saveCount++;
                    yield args[0];
                }
                case "findByMemberIdOrderByCreatedAtDesc" -> byMemberId((String) args[0], false, null);
                case "findByMemberIdAndReadFalseOrderByCreatedAtDesc" -> byMemberId((String) args[0], true, null);
                case "findByMemberIdAndCampaignIdOrderByCreatedAtDesc" -> byMemberId((String) args[0], false,
                        (Long) args[1]);
                case "findByMemberIdAndCampaignIdAndReadFalseOrderByCreatedAtDesc" -> byMemberId((String) args[0],
                        true, (Long) args[1]);
                case "toString" -> "MemberMessageRepositoryStub";
                default -> defaultValue(method.getReturnType());
            };
        }

        private List<MemberMessage> byMemberId(String memberId, boolean unreadOnly, Long campaignId) {
            return messages.stream()
                    .filter(message -> message.getMemberId().equals(memberId))
                    .filter(message -> campaignId == null || message.getCampaign().getId().equals(campaignId))
                    .filter(message -> !unreadOnly || !message.isRead())
                    .toList();
        }
    }

    private static final class TouchDeliveryRepositoryStub implements InvocationHandler {

        private int saveCount;

        TouchDeliveryRepository repository() {
            return (TouchDeliveryRepository) Proxy.newProxyInstance(
                    TouchDeliveryRepository.class.getClassLoader(),
                    new Class<?>[] { TouchDeliveryRepository.class },
                    this
            );
        }

        int saveCount() {
            return saveCount;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "save" -> {
                    saveCount++;
                    yield args[0];
                }
                case "toString" -> "TouchDeliveryRepositoryStub";
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private static final class CampaignEventRepositoryStub implements InvocationHandler {

        private final List<CampaignEvent> savedEvents = new ArrayList<>();

        CampaignEventRepository repository() {
            return (CampaignEventRepository) Proxy.newProxyInstance(
                    CampaignEventRepository.class.getClassLoader(),
                    new Class<?>[] { CampaignEventRepository.class },
                    this
            );
        }

        List<CampaignEvent> savedEvents() {
            return savedEvents;
        }

        int saveCount() {
            return savedEvents.size();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "existsByDeliveryIdAndEventType" -> false;
                case "save" -> {
                    savedEvents.add((CampaignEvent) args[0]);
                    yield args[0];
                }
                case "toString" -> "CampaignEventRepositoryStub";
                default -> defaultValue(method.getReturnType());
            };
        }
    }
}
