package com.example.campaigntouch.touch;

public record DeliveryResult(
        TouchDeliveryStatus status,
        String failedReason
) {
    public static DeliveryResult sent() {
        return new DeliveryResult(TouchDeliveryStatus.SENT, null);
    }

    public static DeliveryResult failed(String failedReason) {
        return new DeliveryResult(TouchDeliveryStatus.FAILED, failedReason);
    }
}
