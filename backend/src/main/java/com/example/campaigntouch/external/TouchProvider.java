package com.example.campaigntouch.external;

import com.example.campaigntouch.touch.DeliveryResult;
import com.example.campaigntouch.touch.TouchChannel;

public interface TouchProvider {

    TouchChannel supportChannel();

    DeliveryResult send(TouchMessageCommand command);
}
