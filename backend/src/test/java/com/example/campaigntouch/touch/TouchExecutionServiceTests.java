package com.example.campaigntouch.touch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.campaigntouch.analytics.CampaignEvent;
import com.example.campaigntouch.analytics.CampaignEventRepository;
import com.example.campaigntouch.analytics.CampaignEventType;
import com.example.campaigntouch.campaign.Campaign;
import com.example.campaigntouch.campaign.CampaignStatus;
import com.example.campaigntouch.campaign.CampaignType;
import com.example.campaigntouch.common.BusinessException;
import com.example.campaigntouch.common.ErrorCode;
import com.example.campaigntouch.external.MemberProfileClient;
import com.example.campaigntouch.external.TouchMessageCommand;
import com.example.campaigntouch.external.TouchProvider;
import com.example.campaigntouch.external.TouchTargetMember;
import com.example.campaigntouch.message.MemberMessage;
import com.example.campaigntouch.message.MemberMessageRepository;
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

class TouchExecutionServiceTests {

    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 6, 18, 0, 0);
    private static final LocalDateTime END_TIME = LocalDateTime.of(2026, 6, 18, 23, 59);

    private TouchTaskRepositoryStub touchTaskRepository;
    private TouchDeliveryRepositoryStub touchDeliveryRepository;
    private MemberMessageRepositoryStub memberMessageRepository;
    private CampaignEventRepositoryStub campaignEventRepository;
    private FakeMemberProfileClient memberProfileClient;

    @BeforeEach
    void setUp() {
        touchTaskRepository = new TouchTaskRepositoryStub();
        touchDeliveryRepository = new TouchDeliveryRepositoryStub();
        memberMessageRepository = new MemberMessageRepositoryStub();
        campaignEventRepository = new CampaignEventRepositoryStub();
        memberProfileClient = new FakeMemberProfileClient();
    }

    @Test
    void executeTouchTaskCreatesDeliveriesForEachMemberAndChannel() {
        TouchTask task = activePendingTask(List.of(TouchChannel.IN_APP, TouchChannel.EMAIL));
        touchTaskRepository.seed(10L, task);
        TouchExecutionService service = service(List.of(sentProvider(TouchChannel.IN_APP), sentProvider(TouchChannel.EMAIL)));

        TouchExecutionResponse response = service.execute(10L);

        assertEquals(TouchTaskStatus.COMPLETED, response.status());
        assertEquals(2, response.targetMemberCount());
        assertEquals(4, response.deliveryCount());
        assertEquals(4, touchDeliveryRepository.savedDeliveries().size());
    }

    @Test
    void executeTouchTaskCreatesMemberMessagesForInAppOnly() {
        TouchTask task = activePendingTask(List.of(TouchChannel.IN_APP, TouchChannel.EMAIL, TouchChannel.PUSH));
        touchTaskRepository.seed(10L, task);
        TouchExecutionService service = service(List.of(
                sentProvider(TouchChannel.IN_APP),
                sentProvider(TouchChannel.EMAIL),
                sentProvider(TouchChannel.PUSH)
        ));

        service.execute(10L);

        assertEquals(6, touchDeliveryRepository.savedDeliveries().size());
        assertEquals(2, memberMessageRepository.savedMessages().size());
    }

    @Test
    void executeTouchTaskCreatesSentAndFailedEvents() {
        TouchTask task = activePendingTask(List.of(TouchChannel.IN_APP, TouchChannel.EMAIL));
        touchTaskRepository.seed(10L, task);
        TouchExecutionService service = service(List.of(
                sentProvider(TouchChannel.IN_APP),
                failedProvider(TouchChannel.EMAIL)
        ));

        TouchExecutionResponse response = service.execute(10L);

        assertEquals(2, response.sentCount());
        assertEquals(2, response.failedCount());
        assertEquals(2, campaignEventRepository.countByEventType(CampaignEventType.SENT));
        assertEquals(2, campaignEventRepository.countByEventType(CampaignEventType.FAILED));
    }

    @Test
    void executeTouchTaskRejectsNonPendingTask() {
        TouchTask task = activePendingTask(List.of(TouchChannel.IN_APP));
        task.markCompleted(LocalDateTime.now());
        touchTaskRepository.seed(10L, task);
        TouchExecutionService service = service(List.of(sentProvider(TouchChannel.IN_APP)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.execute(10L));

        assertEquals(ErrorCode.INVALID_TOUCH_TASK_STATUS, exception.getErrorCode());
    }

    @Test
    void executeTouchTaskRejectsInactiveCampaign() {
        TouchTask task = pendingTask(CampaignStatus.DRAFT, List.of(TouchChannel.IN_APP));
        touchTaskRepository.seed(10L, task);
        TouchExecutionService service = service(List.of(sentProvider(TouchChannel.IN_APP)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.execute(10L));

        assertEquals(ErrorCode.INVALID_CAMPAIGN_STATUS, exception.getErrorCode());
    }

    @Test
    void executeTouchTaskMarksTaskCompletedWhenPartialDeliveryFailed() {
        TouchTask task = activePendingTask(List.of(TouchChannel.IN_APP, TouchChannel.EMAIL));
        touchTaskRepository.seed(10L, task);
        TouchExecutionService service = service(List.of(
                sentProvider(TouchChannel.IN_APP),
                failedProvider(TouchChannel.EMAIL)
        ));

        TouchExecutionResponse response = service.execute(10L);

        assertEquals(TouchTaskStatus.COMPLETED, response.status());
        assertEquals(TouchTaskStatus.COMPLETED, task.getStatus());
        assertEquals(4, response.deliveryCount());
        assertEquals(2, response.sentCount());
        assertEquals(2, response.failedCount());
    }

    @Test
    void executeTouchTaskRejectsMissingTask() {
        TouchExecutionService service = service(List.of(sentProvider(TouchChannel.IN_APP)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.execute(404L));

        assertEquals(ErrorCode.TOUCH_TASK_NOT_FOUND, exception.getErrorCode());
    }

    private TouchExecutionService service(List<TouchProvider> providers) {
        return new TouchExecutionService(
                touchTaskRepository.repository(),
                touchDeliveryRepository.repository(),
                memberMessageRepository.repository(),
                campaignEventRepository.repository(),
                memberProfileClient,
                providers
        );
    }

    private TouchTask activePendingTask(List<TouchChannel> channels) {
        return pendingTask(CampaignStatus.ACTIVE, channels);
    }

    private TouchTask pendingTask(CampaignStatus campaignStatus, List<TouchChannel> channels) {
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
        setId(campaign, 1L);
        TouchTask task = TouchTask.pending(
                campaign,
                "task",
                new AudienceRule(null, null, null, null),
                channels,
                "title",
                "content"
        );
        setId(task, 10L);
        return task;
    }

    private TouchProvider sentProvider(TouchChannel channel) {
        return new FakeTouchProvider(channel, DeliveryResult.sent());
    }

    private TouchProvider failedProvider(TouchChannel channel) {
        return new FakeTouchProvider(channel, DeliveryResult.failed("mock failure"));
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

    private static final class FakeMemberProfileClient implements MemberProfileClient {

        @Override
        public List<TouchTargetMember> queryTargetMembers(AudienceRule audienceRule) {
            return List.of(
                    new TouchTargetMember("1001", "member1001@example.com", "push-1001", "VIP",
                            List.of("3C"), START_TIME, true),
                    new TouchTargetMember("1002", "member1002@example.com", "push-1002", "GOLD",
                            List.of("BEAUTY"), START_TIME, false)
            );
        }
    }

    private record FakeTouchProvider(TouchChannel supportChannel, DeliveryResult result) implements TouchProvider {

        @Override
        public DeliveryResult send(TouchMessageCommand command) {
            return result;
        }
    }

    private static final class TouchTaskRepositoryStub implements InvocationHandler {

        private final List<TouchTask> savedTasks = new ArrayList<>();
        private TouchTask task;

        TouchTaskRepository repository() {
            return (TouchTaskRepository) Proxy.newProxyInstance(
                    TouchTaskRepository.class.getClassLoader(),
                    new Class<?>[] { TouchTaskRepository.class },
                    this
            );
        }

        void seed(Long id, TouchTask seededTask) {
            setId(seededTask, id);
            task = seededTask;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findById" -> Optional.ofNullable(task);
                case "save" -> {
                    savedTasks.add((TouchTask) args[0]);
                    yield args[0];
                }
                case "toString" -> "TouchTaskRepositoryStub";
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private static final class TouchDeliveryRepositoryStub implements InvocationHandler {

        private final List<TouchDelivery> deliveries = new ArrayList<>();
        private long nextId = 1L;

        TouchDeliveryRepository repository() {
            return (TouchDeliveryRepository) Proxy.newProxyInstance(
                    TouchDeliveryRepository.class.getClassLoader(),
                    new Class<?>[] { TouchDeliveryRepository.class },
                    this
            );
        }

        List<TouchDelivery> savedDeliveries() {
            return deliveries;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "save" -> {
                    TouchDelivery delivery = (TouchDelivery) args[0];
                    if (delivery.getId() == null) {
                        setId(delivery, nextId++);
                        deliveries.add(delivery);
                    }
                    yield delivery;
                }
                case "toString" -> "TouchDeliveryRepositoryStub";
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private static final class MemberMessageRepositoryStub implements InvocationHandler {

        private final List<MemberMessage> messages = new ArrayList<>();

        MemberMessageRepository repository() {
            return (MemberMessageRepository) Proxy.newProxyInstance(
                    MemberMessageRepository.class.getClassLoader(),
                    new Class<?>[] { MemberMessageRepository.class },
                    this
            );
        }

        List<MemberMessage> savedMessages() {
            return messages;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "save" -> {
                    messages.add((MemberMessage) args[0]);
                    yield args[0];
                }
                case "toString" -> "MemberMessageRepositoryStub";
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private static final class CampaignEventRepositoryStub implements InvocationHandler {

        private final List<CampaignEvent> events = new ArrayList<>();

        CampaignEventRepository repository() {
            return (CampaignEventRepository) Proxy.newProxyInstance(
                    CampaignEventRepository.class.getClassLoader(),
                    new Class<?>[] { CampaignEventRepository.class },
                    this
            );
        }

        long countByEventType(CampaignEventType eventType) {
            return events.stream().filter(event -> event.getEventType() == eventType).count();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "save" -> {
                    events.add((CampaignEvent) args[0]);
                    yield args[0];
                }
                case "toString" -> "CampaignEventRepositoryStub";
                default -> defaultValue(method.getReturnType());
            };
        }
    }
}
