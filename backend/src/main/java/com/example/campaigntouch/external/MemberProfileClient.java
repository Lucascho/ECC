package com.example.campaigntouch.external;

import com.example.campaigntouch.touch.AudienceRule;
import java.util.List;

public interface MemberProfileClient {

    List<TouchTargetMember> queryTargetMembers(AudienceRule audienceRule);
}
