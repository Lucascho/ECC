package com.example.campaigntouch.external;

import com.example.campaigntouch.touch.DeliveryResult;
import com.example.campaigntouch.touch.TouchChannel;
import org.springframework.stereotype.Component;

@Component
public class InAppTouchProvider implements TouchProvider {

    @Override
    public TouchChannel supportChannel() {
        return TouchChannel.IN_APP;
    }

    @Override
    public DeliveryResult send(TouchMessageCommand command) {
        return DeliveryResult.sent();
    }
}
