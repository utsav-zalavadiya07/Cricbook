package com.example.Cricbook.service;

import com.example.Cricbook.dto.CampaignResponse;
import com.example.Cricbook.dto.CreateCampaignRequest;
import com.example.Cricbook.entity.MarketingCampaign;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.CampaignStatus;
import com.example.Cricbook.enums.NotificationChannel;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.MarketingCampaignRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final MarketingCampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    @org.springframework.context.annotation.Lazy
    private final MonetizationGateService monetizationGateService;

    @Transactional
    public CampaignResponse createCampaign(String username, CreateCampaignRequest request) {
        if (request == null) {
            throw new ApiException("Campaign details are required");
        }
        User creator = findUser(username);

        if (monetizationGateService != null) {
            monetizationGateService.trackAndCheckUsage(creator.getId(), "PROMOTIONS");
        }
        MarketingCampaign campaign = campaignRepository.save(MarketingCampaign.builder()
                .createdBy(creator)
                .name(required(request.getName(), "Campaign name is required"))
                .title(required(request.getTitle(), "Title is required"))
                .message(required(request.getMessage(), "Message is required"))
                .channel(request.getChannel() == null ? NotificationChannel.EMAIL : request.getChannel())
                .targetRole(request.getTargetRole())
                .deepLink(request.getDeepLink())
                .status(request.getScheduledAt() == null ? CampaignStatus.DRAFT : CampaignStatus.SCHEDULED)
                .sentCount(0)
                .scheduledAt(request.getScheduledAt())
                .build());
        return map(campaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> getCampaigns() {
        return campaignRepository.findAllByOrderByCreatedAtDesc().stream().map(this::map).toList();
    }

    @Transactional
    public CampaignResponse sendCampaign(Long campaignId) {
        MarketingCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException("Campaign not found"));
        if (campaign.getStatus() == CampaignStatus.SENT) {
            throw new ApiException("Campaign already sent");
        }

        List<User> users = userRepository.findAll().stream()
                .filter(user -> campaign.getTargetRole() == null || campaign.getTargetRole().isBlank()
                        || user.getRoles().stream().anyMatch(role -> role.getName().name().equals(campaign.getTargetRole())))
                .toList();
        users.forEach(user -> notificationService.pushNotification(
                user.getId(),
                user.getEmail(),
                campaign.getTitle(),
                campaign.getMessage(),
                campaign.getChannel(),
                campaign.getId(),
                campaign.getDeepLink()
        ));
        campaign.setSentCount(users.size());
        campaign.setStatus(CampaignStatus.SENT);
        return map(campaignRepository.save(campaign));
    }

    private CampaignResponse map(MarketingCampaign campaign) {
        return CampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .title(campaign.getTitle())
                .message(campaign.getMessage())
                .channel(campaign.getChannel())
                .targetRole(campaign.getTargetRole())
                .deepLink(campaign.getDeepLink())
                .status(campaign.getStatus())
                .sentCount(campaign.getSentCount())
                .scheduledAt(campaign.getScheduledAt())
                .createdAt(campaign.getCreatedAt())
                .build();
    }

    private User findUser(String loginId) {
        return userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> new ApiException("User not found"));
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
        return value.trim();
    }
}
