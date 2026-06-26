package com.example.campaigntouch.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.example.campaigntouch.analytics.CampaignEventType;
import com.example.campaigntouch.campaign.CampaignStatus;
import com.example.campaigntouch.campaign.CampaignType;
import com.example.campaigntouch.touch.TouchChannel;
import com.example.campaigntouch.touch.TouchDeliveryStatus;
import com.example.campaigntouch.touch.TouchTaskStatus;
import org.junit.jupiter.api.Test;

class EnumValueTests {

    @Test
    void enumValuesMatchPrd() {
        assertArrayEquals(new CampaignStatus[] {
                CampaignStatus.DRAFT,
                CampaignStatus.ACTIVE,
                CampaignStatus.PAUSED,
                CampaignStatus.ENDED
        }, CampaignStatus.values());

        assertArrayEquals(new CampaignType[] {
                CampaignType.PROMOTION,
                CampaignType.NEW_PRODUCT,
                CampaignType.MEMBER_RECALL,
                CampaignType.CART_REMINDER
        }, CampaignType.values());

        assertArrayEquals(new TouchTaskStatus[] {
                TouchTaskStatus.PENDING,
                TouchTaskStatus.PROCESSING,
                TouchTaskStatus.COMPLETED,
                TouchTaskStatus.FAILED
        }, TouchTaskStatus.values());

        assertArrayEquals(new TouchChannel[] {
                TouchChannel.IN_APP,
                TouchChannel.EMAIL,
                TouchChannel.PUSH
        }, TouchChannel.values());

        assertArrayEquals(new TouchDeliveryStatus[] {
                TouchDeliveryStatus.PENDING,
                TouchDeliveryStatus.SENT,
                TouchDeliveryStatus.FAILED,
                TouchDeliveryStatus.CLICKED
        }, TouchDeliveryStatus.values());

        assertArrayEquals(new CampaignEventType[] {
                CampaignEventType.SENT,
                CampaignEventType.FAILED,
                CampaignEventType.CLICK
        }, CampaignEventType.values());
    }
}
