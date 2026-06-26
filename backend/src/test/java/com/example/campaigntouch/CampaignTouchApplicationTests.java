package com.example.campaigntouch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CampaignTouchApplicationTests {

    @Test
    void applicationClassUsesExpectedPackage() {
        assertNotNull(CampaignTouchApplication.class);
        assertEquals("com.example.campaigntouch", CampaignTouchApplication.class.getPackageName());
    }
}
