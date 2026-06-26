package com.example.campaigntouch.external;

import com.example.campaigntouch.touch.DeliveryResult;
import com.example.campaigntouch.touch.TouchChannel;
import org.springframework.stereotype.Component;

@Component
public class MockPushTouchProvider implements TouchProvider {

    @Override
    public TouchChannel supportChannel() {
        return TouchChannel.PUSH;
    }

    @Override
    public DeliveryResult send(TouchMessageCommand command) {
        if (command.pushToken() == null || command.pushToken().isBlank()) {
            return DeliveryResult.failed("Push token is missing.");
        }
        return DeliveryResult.sent();
    }
}
