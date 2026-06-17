package com.example.Cricbook.controller;

import com.example.Cricbook.dto.CampaignResponse;
import com.example.Cricbook.dto.CreateCampaignRequest;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.CampaignStatus;
import com.example.Cricbook.enums.NotificationChannel;
import com.example.Cricbook.repository.*;
import com.example.Cricbook.service.CampaignService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/marketing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('CAMPAIGN_CREATE') or hasAuthority('CMS_EDIT')")
public class AdminMarketingController {

    private final CmsPageRepository cmsPageRepository;
    private final CmsContentRepository cmsContentRepository;
    private final MarketingBannerRepository marketingBannerRepository;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final ReferralRepository referralRepository;
    private final ReferralCampaignRepository referralCampaignRepository;
    private final MarketingCampaignRepository marketingCampaignRepository;
    private final NotificationAnalyticsRepository notificationAnalyticsRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final AnnouncementRepository announcementRepository;
    private final LandingPageRepository landingPageRepository;
    private final SeoSettingRepository seoSettingRepository;
    private final UserSegmentRepository userSegmentRepository;
    private final CampaignService campaignService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        List<MarketingCampaign> campaigns = marketingCampaignRepository.findAllByOrderByCreatedAtDesc();
        long activeCampaigns = campaigns.stream().filter(campaign -> campaign.getStatus() == CampaignStatus.SCHEDULED || campaign.getStatus() == CampaignStatus.SENT).count();
        long sends = campaigns.stream().map(MarketingCampaign::getSentCount).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        long views = notificationAnalyticsRepository.findAll().stream().map(NotificationAnalytics::getViews).filter(Objects::nonNull).mapToLong(Long::longValue).sum();
        long clicks = notificationAnalyticsRepository.findAll().stream().map(NotificationAnalytics::getClicks).filter(Objects::nonNull).mapToLong(Long::longValue).sum();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("activeCampaigns", activeCampaigns);
        response.put("couponUsage", couponUsageRepository.count());
        response.put("referralRevenue", referralRepository.findAll().stream().filter(referral -> "COMPLETED".equalsIgnoreCase(referral.getStatus())).count() * 100);
        response.put("pushDeliveryRate", sends == 0 ? 0 : 100);
        response.put("emailOpenRate", percent(views, Math.max(sends, 1)));
        response.put("campaignRoi", campaigns.isEmpty() ? 0 : Math.max(0, clicks * 5 - campaigns.size()));
        response.put("aiInsights", marketingInsights(activeCampaigns, clicks, sends));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cms-pages")
    public ResponseEntity<List<CmsPage>> cmsPages() {
        return ResponseEntity.ok(cmsPageRepository.findAllByOrderByUpdatedAtDesc());
    }

    @PostMapping("/cms-pages")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('CMS_EDIT')")
    public ResponseEntity<CmsPage> saveCmsPage(@RequestBody CmsPage request) {
        CmsPage page = cmsPageRepository.findBySlug(request.getSlug()).orElseGet(CmsPage::new);
        page.setSlug(required(request.getSlug(), "CMS slug is required"));
        page.setTitle(required(request.getTitle(), "CMS title is required"));
        page.setContent(defaultString(request.getContent(), ""));
        page.setStatus(defaultString(request.getStatus(), "DRAFT").toUpperCase(Locale.ROOT));
        page.setVersionNumber(page.getVersionNumber() == null ? 1 : page.getVersionNumber() + 1);
        return ResponseEntity.ok(cmsPageRepository.save(page));
    }

    @GetMapping("/cms-content")
    public ResponseEntity<List<CmsContent>> cmsContent() {
        return ResponseEntity.ok(cmsContentRepository.findAll());
    }

    @GetMapping("/banners")
    public ResponseEntity<List<MarketingBanner>> banners() {
        return ResponseEntity.ok(marketingBannerRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/banners")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('CMS_EDIT')")
    public ResponseEntity<MarketingBanner> saveBanner(@RequestBody MarketingBanner request) {
        MarketingBanner banner = request.getId() == null ? new MarketingBanner() : marketingBannerRepository.findById(request.getId()).orElseGet(MarketingBanner::new);
        banner.setTitle(required(request.getTitle(), "Banner title is required"));
        banner.setLocation(defaultString(request.getLocation(), "HOMEPAGE").toUpperCase(Locale.ROOT));
        banner.setBannerType(defaultString(request.getBannerType(), "IMAGE").toUpperCase(Locale.ROOT));
        banner.setImageUrl(required(request.getImageUrl(), "Banner image is required"));
        banner.setTargetUrl(request.getTargetUrl());
        banner.setStartDate(request.getStartDate());
        banner.setEndDate(request.getEndDate());
        banner.setStatus(defaultString(request.getStatus(), "DRAFT").toUpperCase(Locale.ROOT));
        return ResponseEntity.ok(marketingBannerRepository.save(banner));
    }

    @GetMapping("/coupons")
    public ResponseEntity<List<Map<String, Object>>> coupons() {
        return ResponseEntity.ok(couponRepository.findAll().stream().map(this::mapCoupon).toList());
    }

    @PostMapping("/coupons")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('COUPON_CREATE')")
    public ResponseEntity<Map<String, Object>> saveCoupon(@RequestBody CouponRequest request) {
        Coupon coupon = request.getId() == null ? new Coupon() : couponRepository.findById(request.getId()).orElseGet(Coupon::new);
        coupon.setCode(required(request.getCode(), "Coupon code is required").toUpperCase(Locale.ROOT));
        coupon.setDiscountType(defaultString(request.getDiscountType(), "FLAT").toUpperCase(Locale.ROOT));
        coupon.setDiscountValue(request.getDiscountValue() == null ? BigDecimal.ZERO : request.getDiscountValue());
        coupon.setMaxDiscount(request.getMaxDiscount());
        coupon.setMinBookingAmount(request.getMinBookingAmount());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setActive(request.getActive() == null || request.getActive());
        return ResponseEntity.ok(mapCoupon(couponRepository.save(coupon)));
    }

    @GetMapping("/referral-campaigns")
    public ResponseEntity<List<ReferralCampaign>> referralCampaigns() {
        return ResponseEntity.ok(referralCampaignRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/referral-campaigns")
    public ResponseEntity<ReferralCampaign> saveReferralCampaign(@RequestBody ReferralCampaign request) {
        ReferralCampaign campaign = request.getId() == null ? new ReferralCampaign() : referralCampaignRepository.findById(request.getId()).orElseGet(ReferralCampaign::new);
        campaign.setName(required(request.getName(), "Referral campaign name is required"));
        campaign.setReward(request.getReward() == null ? BigDecimal.ZERO : request.getReward());
        campaign.setEligibility(defaultString(request.getEligibility(), "ALL_USERS"));
        campaign.setExpiresAt(request.getExpiresAt());
        campaign.setStatus(defaultString(request.getStatus(), "ACTIVE").toUpperCase(Locale.ROOT));
        return ResponseEntity.ok(referralCampaignRepository.save(campaign));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<List<Map<String, Object>>> campaigns() {
        return ResponseEntity.ok(marketingCampaignRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapCampaign).toList());
    }

    @PostMapping("/campaigns")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('CAMPAIGN_CREATE') or hasAuthority('EMAIL_SEND') or hasAuthority('PUSH_SEND')")
    public ResponseEntity<CampaignResponse> createCampaign(@RequestBody CreateCampaignRequest request) {
        MarketingCampaign campaign = marketingCampaignRepository.save(MarketingCampaign.builder()
                .createdBy(marketingActor())
                .name(required(request.getName(), "Campaign name is required"))
                .title(required(request.getTitle(), "Campaign title is required"))
                .message(required(request.getMessage(), "Campaign message is required"))
                .channel(request.getChannel() == null ? NotificationChannel.EMAIL : request.getChannel())
                .targetRole(request.getTargetRole())
                .deepLink(request.getDeepLink())
                .status(request.getScheduledAt() == null ? CampaignStatus.DRAFT : CampaignStatus.SCHEDULED)
                .sentCount(0)
                .scheduledAt(request.getScheduledAt())
                .build());
        return ResponseEntity.ok(CampaignResponse.builder()
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
                .build());
    }

    @PostMapping("/campaigns/{id}/send")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('EMAIL_SEND') or hasAuthority('PUSH_SEND')")
    public ResponseEntity<CampaignResponse> sendCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.sendCampaign(id));
    }

    @GetMapping("/templates")
    public ResponseEntity<List<NotificationTemplate>> templates() {
        return ResponseEntity.ok(notificationTemplateRepository.findAll());
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('EMAIL_SEND') or hasAuthority('PUSH_SEND')")
    public ResponseEntity<NotificationTemplate> saveTemplate(@RequestBody NotificationTemplate request) {
        NotificationTemplate template = request.getId() == null ? new NotificationTemplate() : notificationTemplateRepository.findById(request.getId()).orElseGet(NotificationTemplate::new);
        template.setName(required(request.getName(), "Template name is required"));
        template.setSubject(request.getSubject());
        template.setBody(required(request.getBody(), "Template body is required"));
        template.setChannel(request.getChannel() == null ? NotificationChannel.EMAIL : request.getChannel());
        return ResponseEntity.ok(notificationTemplateRepository.save(template));
    }

    @GetMapping("/announcements")
    public ResponseEntity<List<Announcement>> announcements() {
        return ResponseEntity.ok(announcementRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/announcements")
    public ResponseEntity<Announcement> saveAnnouncement(@RequestBody Announcement request) {
        Announcement announcement = request.getId() == null ? new Announcement() : announcementRepository.findById(request.getId()).orElseGet(Announcement::new);
        announcement.setTitle(required(request.getTitle(), "Announcement title is required"));
        announcement.setMessage(required(request.getMessage(), "Announcement message is required"));
        announcement.setPriority(defaultString(request.getPriority(), "NORMAL").toUpperCase(Locale.ROOT));
        announcement.setDisplayLocation(defaultString(request.getDisplayLocation(), "BANNER").toUpperCase(Locale.ROOT));
        announcement.setStatus(defaultString(request.getStatus(), "DRAFT").toUpperCase(Locale.ROOT));
        return ResponseEntity.ok(announcementRepository.save(announcement));
    }

    @GetMapping("/landing-pages")
    public ResponseEntity<List<LandingPage>> landingPages() {
        return ResponseEntity.ok(landingPageRepository.findAllByOrderByUpdatedAtDesc());
    }

    @PostMapping("/landing-pages")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('CMS_EDIT')")
    public ResponseEntity<LandingPage> saveLandingPage(@RequestBody LandingPage request) {
        LandingPage page = landingPageRepository.findBySlug(request.getSlug()).orElseGet(LandingPage::new);
        page.setSlug(required(request.getSlug(), "Landing page slug is required"));
        page.setTitle(required(request.getTitle(), "Landing page title is required"));
        page.setTemplateName(defaultString(request.getTemplateName(), "DEFAULT"));
        page.setContentJson(defaultString(request.getContentJson(), "{}"));
        page.setStatus(defaultString(request.getStatus(), "DRAFT").toUpperCase(Locale.ROOT));
        return ResponseEntity.ok(landingPageRepository.save(page));
    }

    @GetMapping("/seo")
    public ResponseEntity<List<SeoSetting>> seoSettings() {
        return ResponseEntity.ok(seoSettingRepository.findAllByOrderByUpdatedAtDesc());
    }

    @PostMapping("/seo")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('SEO_EDIT')")
    public ResponseEntity<SeoSetting> saveSeo(@RequestBody SeoSetting request) {
        SeoSetting setting = seoSettingRepository.findByPageSlug(request.getPageSlug()).orElseGet(SeoSetting::new);
        setting.setPageSlug(required(request.getPageSlug(), "Page slug is required"));
        setting.setMetaTitle(required(request.getMetaTitle(), "Meta title is required"));
        setting.setMetaDescription(required(request.getMetaDescription(), "Meta description is required"));
        setting.setKeywords(request.getKeywords());
        setting.setOpenGraphImage(request.getOpenGraphImage());
        return ResponseEntity.ok(seoSettingRepository.save(setting));
    }

    @GetMapping("/segments")
    public ResponseEntity<List<UserSegment>> segments() {
        return ResponseEntity.ok(userSegmentRepository.findAll());
    }

    @PostMapping("/segments")
    public ResponseEntity<UserSegment> saveSegment(@RequestBody UserSegment request) {
        UserSegment segment = request.getId() == null ? new UserSegment() : userSegmentRepository.findById(request.getId()).orElseGet(UserSegment::new);
        segment.setName(required(request.getName(), "Segment name is required"));
        segment.setRuleType(defaultString(request.getRuleType(), "BOOKINGS_COUNT"));
        segment.setRuleValue(defaultString(request.getRuleValue(), "1"));
        return ResponseEntity.ok(userSegmentRepository.save(segment));
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> analytics() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("couponUsage", couponUsageRepository.count());
        response.put("referralConversion", percent(referralRepository.findAll().stream().filter(referral -> "COMPLETED".equalsIgnoreCase(referral.getStatus())).count(), Math.max(referralRepository.count(), 1)));
        response.put("campaignRoi", notificationAnalyticsRepository.findAll().stream().map(NotificationAnalytics::getClicks).filter(Objects::nonNull).mapToLong(Long::longValue).sum() * 5);
        response.put("pushOpenRate", channelOpenRate(NotificationChannel.PUSH));
        response.put("emailOpenRate", channelOpenRate(NotificationChannel.EMAIL));
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> mapCoupon(Coupon coupon) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", coupon.getId());
        item.put("code", coupon.getCode());
        item.put("discountType", coupon.getDiscountType());
        item.put("discountValue", coupon.getDiscountValue());
        item.put("maxDiscount", coupon.getMaxDiscount());
        item.put("minBookingAmount", coupon.getMinBookingAmount());
        item.put("validFrom", coupon.getValidFrom());
        item.put("validUntil", coupon.getValidUntil());
        item.put("active", coupon.getActive());
        item.put("usageCount", couponUsageRepository.findAll().stream().filter(usage -> usage.getCoupon().getId().equals(coupon.getId())).count());
        return item;
    }

    private Map<String, Object> mapCampaign(MarketingCampaign campaign) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", campaign.getId());
        item.put("name", campaign.getName());
        item.put("title", campaign.getTitle());
        item.put("message", campaign.getMessage());
        item.put("channel", campaign.getChannel().name());
        item.put("targetRole", campaign.getTargetRole());
        item.put("deepLink", campaign.getDeepLink());
        item.put("status", campaign.getStatus().name());
        item.put("sentCount", campaign.getSentCount());
        item.put("scheduledAt", campaign.getScheduledAt());
        item.put("createdAt", campaign.getCreatedAt());
        return item;
    }

    private double channelOpenRate(NotificationChannel channel) {
        List<MarketingCampaign> campaigns = marketingCampaignRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(campaign -> campaign.getChannel() == channel)
                .toList();
        long sends = campaigns.stream().map(MarketingCampaign::getSentCount).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        long views = campaigns.stream()
                .map(campaign -> notificationAnalyticsRepository.findByCampaignId(campaign.getId()).map(NotificationAnalytics::getViews).orElse(0L))
                .mapToLong(Long::longValue)
                .sum();
        return percent(views, Math.max(sends, 1));
    }

    private List<Map<String, Object>> marketingInsights(long activeCampaigns, long clicks, long sends) {
        List<Map<String, Object>> insights = new ArrayList<>();
        if (activeCampaigns == 0) {
            insights.add(insight("No active growth campaigns", "Schedule at least one push or email campaign for retention.", "HIGH"));
        }
        if (sends > 0 && percent(clicks, sends) < 5) {
            insights.add(insight("Low campaign engagement", "Open/click performance is below 5%; review segment targeting and copy.", "MEDIUM"));
        }
        if (couponUsageRepository.count() == 0 && couponRepository.count() > 0) {
            insights.add(insight("Coupons not converting", "Active coupons exist but usage is still zero.", "MEDIUM"));
        }
        if (insights.isEmpty()) {
            insights.add(insight("Marketing engine stable", "Campaign, coupon, and referral signals are within expected range.", "LOW"));
        }
        return insights;
    }

    private Map<String, Object> insight(String title, String message, String severity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", title);
        item.put("message", message);
        item.put("severity", severity);
        return item;
    }

    private double percent(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(value * 100.0 / total).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private String actor() {
        return SecurityContextHolder.getContext().getAuthentication() == null
                ? "admin@cricbook.com"
                : SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private User marketingActor() {
        return userRepository.findByEmailOrPhone(actor())
                .or(() -> userRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("At least one platform user is required to create campaigns"));
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    @Data
    public static class CouponRequest {
        private Long id;
        private String code;
        private String discountType;
        private BigDecimal discountValue;
        private BigDecimal maxDiscount;
        private BigDecimal minBookingAmount;
        private LocalDate validFrom;
        private LocalDate validUntil;
        private Boolean active;
    }
}
