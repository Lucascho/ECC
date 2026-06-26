package com.example.campaigntouch.external;

import com.example.campaigntouch.touch.DeliveryResult;
import com.example.campaigntouch.touch.TouchChannel;
import org.springframework.stereotype.Component;

@Component
public class MockEmailTouchProvider implements TouchProvider {

    @Override
    public TouchChannel supportChannel() {
        return TouchChannel.EMAIL;
    }

    @Override
    public DeliveryResult send(TouchMessageCommand command) {
        if (command.email() == null || command.email().isBlank()) {
            return DeliveryResult.failed("Email is missing.");
        }
        return DeliveryResult.sent();
    }
}
