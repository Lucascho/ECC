package com.example.campaigntouch.message;

import com.example.campaigntouch.common.BusinessException;
import com.example.campaigntouch.common.ErrorCode;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member/messages")
public class MemberMessageController {

    private final MemberMessageService memberMessageService;

    public MemberMessageController(MemberMessageService memberMessageService) {
        this.memberMessageService = memberMessageService;
    }

    @GetMapping
    public List<MemberMessageResponse> list(
            @RequestHeader(value = "X-Member-Id", required = false) String memberId,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) Boolean unreadOnly
    ) {
        return memberMessageService.list(requireMemberId(memberId), campaignId, unreadOnly);
    }

    @PostMapping("/{messageId}/click")
    public MemberMessageClickResponse click(
            @RequestHeader(value = "X-Member-Id", required = false) String memberId,
            @PathVariable Long messageId
    ) {
        return memberMessageService.click(messageId, requireMemberId(memberId));
    }

    private String requireMemberId(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            throw new BusinessException(ErrorCode.MEMBER_ID_REQUIRED);
        }
        return memberId;
    }
}
