package com.example.Cricbook.config;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.repository.*;
import com.example.Cricbook.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Component
@Order(50)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cricbook.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class AdminDemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TurfRepository turfRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final OwnerVerificationRepository ownerVerificationRepository;
    private final TurfModerationRepository turfModerationRepository;
    private final RefundRepository refundRepository;
    private final OwnerSettlementRepository ownerSettlementRepository;
    private final CommissionRuleRepository commissionRuleRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ReconciliationLogRepository reconciliationLogRepository;
    private final TaxReportRepository taxReportRepository;
    private final FinanceDisputeRepository financeDisputeRepository;
    private final FinanceAuditRepository financeAuditRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final BookingCancellationRequestRepository cancellationRequestRepository;
    private final BookingDisputeRepository bookingDisputeRepository;
    private final SlotConflictRepository slotConflictRepository;
    private final NoShowCaseRepository noShowCaseRepository;
    private final WeatherRefundRepository weatherRefundRepository;
    private final BookingTimelineRepository bookingTimelineRepository;
    private final BookingEscalationCaseRepository bookingEscalationCaseRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final SupportAgentRepository supportAgentRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final FaqRepository faqRepository;
    private final CsatFeedbackRepository csatFeedbackRepository;
    private final TicketNoteRepository ticketNoteRepository;
    private final TicketEscalationRepository ticketEscalationRepository;
    private final SupportChatMessageRepository supportChatMessageRepository;
    private final CmsContentRepository cmsContentRepository;
    private final CmsPageRepository cmsPageRepository;
    private final MarketingBannerRepository marketingBannerRepository;
    private final CouponRepository couponRepository;
    private final ReferralCampaignRepository referralCampaignRepository;
    private final MarketingCampaignRepository marketingCampaignRepository;
    private final AnnouncementRepository announcementRepository;
    private final SeoSettingRepository seoSettingRepository;
    private final PlatformSettingRepository platformSettingRepository;
    private final FeatureFlagRepository featureFlagRepository;
    private final ReleaseVersionRepository releaseVersionRepository;
    private final ConfigurationAuditRepository configurationAuditRepository;
    private final TrustReportRepository trustReportRepository;
    private final PlayerTrustScoreRepository playerTrustScoreRepository;
    private final UserWarningRepository userWarningRepository;
    private final SafetySuspensionRepository safetySuspensionRepository;
    private final BannedEntityRepository bannedEntityRepository;
    private final ModerationAppealRepository moderationAppealRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final FraudProfileRepository fraudProfileRepository;
    private final TeamRepository teamRepository;
    private final ReviewRepository reviewRepository;
    private final TeamModerationRepository teamModerationRepository;
    private final ReviewModerationRepository reviewModerationRepository;
    private final CommunityModerationRepository communityModerationRepository;
    private final TrustAdjustmentRepository trustAdjustmentRepository;
    private final PermissionAuditRepository permissionAuditRepository;
    private final IncidentRepository incidentRepository;
    private final AuditLogRepository auditLogRepository;
    private final HolidayService holidayService;

    @Override
    @Transactional
    public void run(String... args) {
        holidayService.seedDefaultHolidays();
        Role userRole = role(RoleName.ROLE_USER);
        Role ownerRole = role(RoleName.ROLE_OWNER);

        User player = user("demo.player@cricbook.com", "Demo Player", "9876500001", UserStatus.ACTIVE, userRole);
        User owner = user("demo.owner@cricbook.com", "Demo Owner", "9876500002", UserStatus.ACTIVE, ownerRole);
        User riskUser = user("demo.risk@cricbook.com", "Flagged Demo Player", "9876500003", UserStatus.BLOCKED, userRole);

        Turf activeTurf = turf(owner, "Green Box Arena", "SG Highway", "Ahmedabad", TurfStatus.ACTIVE, BigDecimal.valueOf(1500));
        Turf pendingTurf = turf(owner, "Night Lights Cricket", "Vesu", "Surat", TurfStatus.INACTIVE, BigDecimal.valueOf(2200));
        Slot slot = slot(activeTurf);
        Booking booking = booking(player, activeTurf, slot);
        Payment payment = payment(booking);

        seedUserOwnerTurfFlow(owner, pendingTurf);
        seedBookingOps(player, riskUser, booking, slot);
        seedFinance(player, owner, booking, payment);
        seedSupport(player);
        seedMarketing(owner);
        seedSystemAdmin();
        seedSafety(player, riskUser, booking, activeTurf);
        seedOpsShell();
    }

    private Role role(RoleName roleName) {
        return roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
    }

    private User user(String email, String name, String phone, UserStatus status, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> userRepository.save(User.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .password(passwordEncoder.encode("demo123"))
                .status(status)
                .roles(new HashSet<>(Set.of(role)))
                .build()));
    }

    private Turf turf(User owner, String name, String location, String city, TurfStatus status, BigDecimal price) {
        return turfRepository.findAll().stream()
                .filter(turf -> name.equalsIgnoreCase(turf.getName()))
                .findFirst()
                .orElseGet(() -> turfRepository.save(Turf.builder()
                        .owner(owner)
                        .name(name)
                        .description("Backend-seeded demo turf for admin portal flows.")
                        .location(location)
                        .city(city)
                        .country("India")
                        .state("Gujarat")
                        .timezone("Asia/Kolkata")
                        .holidaySource("India + Gujarat Calendar")
                        .latitude(23.0225)
                        .longitude(72.5714)
                        .pricePerHour(price)
                        .pitchType(PitchType.TURF)
                        .boxType(BoxType.SIX_V_SIX)
                        .status(status)
                        .rating(4.6)
                        .totalReviews(28)
                        .build()));
    }

    private Slot slot(Turf turf) {
        return slotRepository.findAll().stream()
                .filter(slot -> slot.getTurf().getId().equals(turf.getId()) && slot.getStartTime().equals(LocalTime.of(19, 0)))
                .findFirst()
                .orElseGet(() -> slotRepository.save(Slot.builder()
                        .turf(turf)
                        .startTime(LocalTime.of(19, 0))
                        .endTime(LocalTime.of(21, 0))
                        .price(BigDecimal.valueOf(3000))
                        .isActive(true)
                        .build()));
    }

    private Booking booking(User player, Turf turf, Slot slot) {
        LocalDate date = LocalDate.now().plusDays(2);
        return bookingRepository.findAll().stream()
                .filter(booking -> booking.getSlot().getId().equals(slot.getId()) && booking.getBookingDate().equals(date))
                .findFirst()
                .orElseGet(() -> bookingRepository.save(Booking.builder()
                        .user(player)
                        .turf(turf)
                        .slot(slot)
                        .bookingDate(date)
                        .status(BookingStatus.CONFIRMED)
                        .amount(BigDecimal.valueOf(3000))
                        .expiryTime(LocalDateTime.now().plusDays(1))
                        .build()));
    }

    private Payment payment(Booking booking) {
        return paymentRepository.findAll().stream()
                .filter(payment -> "pay_demo_admin_001".equals(payment.getRazorpayPaymentId()))
                .findFirst()
                .orElseGet(() -> paymentRepository.save(Payment.builder()
                        .booking(booking)
                        .razorpayOrderId("order_demo_admin_001")
                        .razorpayPaymentId("pay_demo_admin_001")
                        .status("SUCCESS")
                        .amount(BigDecimal.valueOf(3000))
                        .build()));
    }

    private void seedUserOwnerTurfFlow(User owner, Turf pendingTurf) {
        if (ownerVerificationRepository.count() == 0) {
            ownerVerificationRepository.save(OwnerVerification.builder()
                    .ownerId(owner.getId())
                    .documents("GST, PAN, business license and identity proof uploaded for demo review.")
                    .status("PENDING")
                    .build());
        }
        if (turfModerationRepository.count() == 0) {
            turfModerationRepository.save(TurfModeration.builder()
                    .turfId(pendingTurf.getId())
                    .status("PENDING")
                    .reason("Photo, amenities, and pricing verification required.")
                    .build());
        }
    }

    private void seedBookingOps(User player, User riskUser, Booking booking, Slot slot) {
        if (cancellationRequestRepository.count() == 0) {
            cancellationRequestRepository.save(BookingCancellationRequest.builder().booking(booking).requestedBy("PLAYER").cancellationType("PLAYER_CANCELLATION").reason("Team unavailable for booked slot.").status("PENDING").build());
        }
        if (bookingDisputeRepository.count() == 0) {
            bookingDisputeRepository.save(BookingDispute.builder().booking(booking).disputeType("PAYMENT").status("OPEN").summary("Player claims amount deducted twice.").assignedTo("support-admin").build());
        }
        if (slotConflictRepository.count() == 0) {
            slotConflictRepository.save(SlotConflict.builder().slotId(slot.getId()).bookingA(booking.getId()).bookingB(booking.getId() + 1).status("OPEN").notes("Demo overlapping slot conflict for operations review.").build());
        }
        if (noShowCaseRepository.count() == 0) {
            noShowCaseRepository.save(NoShowCase.builder().booking(booking).user(riskUser).noShowType("PLAYER").status("OPEN").penalty(BigDecimal.valueOf(250)).notes("Repeated no-show sample case.").build());
        }
        if (weatherRefundRepository.count() == 0) {
            weatherRefundRepository.save(WeatherRefund.builder().booking(booking).refundType(WeatherRefundType.WALLET_CREDIT).amount(BigDecimal.valueOf(1500)).status(WeatherRefundStatus.PENDING).reason("Heavy rain alert during booked slot.").build());
        }
        if (bookingTimelineRepository.count() == 0) {
            bookingTimelineRepository.save(BookingTimeline.builder().booking(booking).eventType("PAYMENT_CAPTURED").performedBy("System").details("₹3000 payment captured for demo booking.").build());
        }
        if (bookingEscalationCaseRepository.count() == 0) {
            bookingEscalationCaseRepository.save(BookingEscalationCase.builder().booking(booking).ticketId(701L).priority("HIGH").assignedTo("ops-lead").status("OPEN").summary("Resolve slot/payment conflict before match time.").build());
        }
    }

    private void seedFinance(User player, User owner, Booking booking, Payment payment) {
        if (refundRepository.count() == 0) refundRepository.save(Refund.builder().payment(payment).amount(BigDecimal.valueOf(1500)).status("PENDING").reason("Weather disruption").build());
        if (ownerSettlementRepository.count() == 0) ownerSettlementRepository.save(OwnerSettlement.builder().owner(owner).booking(booking).grossAmount(BigDecimal.valueOf(3000)).commission(BigDecimal.valueOf(300)).netAmount(BigDecimal.valueOf(2700)).status("PENDING").reference("settle_demo_001").build());
        if (commissionRuleRepository.count() == 0) commissionRuleRepository.save(CommissionRule.builder().type("PERCENT").value(BigDecimal.TEN).description("Default platform commission").active(true).build());
        if (walletRepository.findAll().stream().noneMatch(wallet -> wallet.getUser().getId().equals(player.getId()))) {
            Wallet wallet = walletRepository.save(Wallet.builder().user(player).balance(BigDecimal.valueOf(750)).build());
            walletTransactionRepository.save(WalletTransaction.builder().wallet(wallet).amount(BigDecimal.valueOf(750)).type("WEATHER_CREDIT").status("SUCCESS").reference("wallet_demo_001").description("Sample wallet credit").build());
        }
        if (reconciliationLogRepository.count() == 0) reconciliationLogRepository.save(ReconciliationLog.builder().transactionId("pay_demo_admin_001").gatewayAmount(BigDecimal.valueOf(3000)).systemAmount(BigDecimal.valueOf(3000)).status("MATCHED").notes("Sample matched transaction").build());
        if (taxReportRepository.count() == 0) taxReportRepository.save(TaxReport.builder().reportType("GST_SUMMARY").period("2026-05").taxableAmount(BigDecimal.valueOf(14830)).gstAmount(BigDecimal.valueOf(2670)).build());
        if (financeDisputeRepository.count() == 0) financeDisputeRepository.save(FinanceDispute.builder().userId(player.getId()).bookingId(booking.getId()).type("DOUBLE_DEBIT").status("OPEN").amount(BigDecimal.valueOf(3000)).summary("Customer reports duplicate debit.").build());
        if (financeAuditRepository.count() == 0) financeAuditRepository.save(FinanceAudit.builder().action("REFUND_REVIEWED").actor("admin@cricbook.com").userId(player.getId()).entity("Booking #" + booking.getId()).details("Sample refund reviewed by finance admin.").build());
        if (fraudAlertRepository.count() == 0) fraudAlertRepository.save(FraudAlert.builder().userId(player.getId()).severity("HIGH").reason("Multiple refund requests in short window.").build());
    }

    private void seedSupport(User player) {
        if (supportTicketRepository.count() == 0) supportTicketRepository.save(SupportTicket.builder().userId(player.getId()).ticketNumber("TCK-DEMO-701").category("Payment Issue").priority("HIGH").assignedTo("support-agent").channel("IN_APP").issue("Payment deducted but booking still pending.").status("OPEN").chatLog("User: Payment deducted but booking pending.\\nAgent: We are checking gateway status.").build());
        if (supportAgentRepository.count() == 0) supportAgentRepository.save(SupportAgent.builder().userId(9001L).name("Support Agent").role("L1 Support").status("ONLINE").build());
        if (slaPolicyRepository.count() == 0) slaPolicyRepository.save(SlaPolicy.builder().priority("HIGH").responseTimeHours(4).resolutionTimeHours(24).build());
        if (knowledgeArticleRepository.count() == 0) knowledgeArticleRepository.save(KnowledgeArticle.builder().title("How refunds work").content("Refunds can be processed to source or wallet based on rules.").category("Payments").status("PUBLISHED").build());
        if (faqRepository.count() == 0) faqRepository.save(Faq.builder().question("How do I cancel a booking?").answer("Open booking details and request cancellation before the configured window.").category("Bookings").status("PUBLISHED").build());
        if (csatFeedbackRepository.count() == 0) csatFeedbackRepository.save(CsatFeedback.builder().ticketId(701L).rating(5).feedback("Quick demo response.").build());
        if (ticketNoteRepository.count() == 0) ticketNoteRepository.save(TicketNote.builder().ticketId(701L).agentId("support-agent").note("Gateway status check required.").build());
        if (ticketEscalationRepository.count() == 0) ticketEscalationRepository.save(TicketEscalation.builder().ticketId(701L).level("L2").assignedTo("finance-admin").status("OPEN").reason("Payment mismatch needs finance review.").build());
        if (supportChatMessageRepository.count() == 0) supportChatMessageRepository.save(SupportChatMessage.builder().ticketId(701L).senderId(player.getId()).senderType("USER").message("Please confirm my booking status.").build());
    }

    private void seedMarketing(User owner) {
        if (cmsContentRepository.count() == 0) cmsContentRepository.save(CmsContent.builder().type("BANNER").title("Weekend Cricket Offer").imageUrl("/demo-banner.jpg").status("ACTIVE").build());
        if (cmsPageRepository.count() == 0) cmsPageRepository.save(CmsPage.builder().slug("refund-policy-demo").title("Refund Policy Demo").content("Demo CMS page content for admin preview.").status("PUBLISHED").versionNumber(1).build());
        if (marketingBannerRepository.count() == 0) marketingBannerRepository.save(MarketingBanner.builder().title("Weekend Cricket Offer").location("Homepage").bannerType("Image").imageUrl("/demo-banner.jpg").targetUrl("/search").status("ACTIVE").startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(14)).build());
        if (couponRepository.count() == 0) couponRepository.save(Coupon.builder().code("WELCOME100").discountType("FLAT").discountValue(BigDecimal.valueOf(100)).maxDiscount(BigDecimal.valueOf(100)).minBookingAmount(BigDecimal.valueOf(500)).validFrom(LocalDate.now()).validUntil(LocalDate.now().plusMonths(1)).active(true).build());
        if (referralCampaignRepository.count() == 0) referralCampaignRepository.save(ReferralCampaign.builder().name("Demo Referral Rewards").reward(BigDecimal.valueOf(100)).eligibility("FIRST_BOOKING").status("ACTIVE").expiresAt(LocalDateTime.now().plusMonths(1)).build());
        if (marketingCampaignRepository.count() == 0) marketingCampaignRepository.save(MarketingCampaign.builder().createdBy(owner).name("Weekend Push").title("Book cricket this weekend").message("Save on prime slots with WELCOME100.").channel(NotificationChannel.PUSH).targetRole("USER").deepLink("/search").status(CampaignStatus.DRAFT).sentCount(0).build());
        if (announcementRepository.count() == 0) announcementRepository.save(Announcement.builder().title("Demo Maintenance Window").message("Sample announcement for admin marketing center.").priority("MEDIUM").displayLocation("BANNER").status("ACTIVE").build());
        if (seoSettingRepository.count() == 0) seoSettingRepository.save(SeoSetting.builder().pageSlug("home").metaTitle("Cricbook Demo").metaDescription("Demo SEO settings created by backend seeder.").keywords("cricket,turf,booking").openGraphImage("/demo-og.jpg").build());
    }

    private void seedSystemAdmin() {
        setting("GLOBAL_PLATFORM_NAME", "Cricbook");
        setting("PAYMENT_PLATFORM_COMMISSION", "10");
        setting("BOOKING_ADVANCE_DAYS", "30");
        if (featureFlagRepository.count() == 0) featureFlagRepository.save(FeatureFlag.builder().featureKey("ENABLE_TOURNAMENTS").enabled(true).rolloutPercentage(100).targetAudience("ALL").description("Demo feature rollout").build());
        if (releaseVersionRepository.count() == 0) releaseVersionRepository.save(ReleaseVersion.builder().version("2.5-demo").notes("Demo release for admin control center.").status("ROLLOUT").rolloutPercentage(25).releaseDate(LocalDate.now()).build());
        if (configurationAuditRepository.count() == 0) configurationAuditRepository.save(ConfigurationAudit.builder().configKey("PAYMENT_PLATFORM_COMMISSION").oldValue("8").newValue("10").changedBy("admin@cricbook.com").build());
    }

    private void seedSafety(User player, User riskUser, Booking booking, Turf turf) {
        if (playerTrustScoreRepository.findByUserId(riskUser.getId()).isEmpty()) playerTrustScoreRepository.save(PlayerTrustScore.builder().user(riskUser).score(38).attendanceScore(35).punctualityScore(40).cancellationScore(55).reportScore(30).paymentScore(75).matchCompletionScore(45).build());
        if (trustReportRepository.count() == 0) trustReportRepository.save(TrustReport.builder().reporter(player).reportedUser(riskUser).booking(booking).reason(TrustReportReason.MISCONDUCT).description("Repeated no-show and abusive chat behavior.").status(TrustReportStatus.OPEN).build());
        if (userWarningRepository.count() == 0) userWarningRepository.save(UserWarning.builder().user(riskUser).warningLevel("LEVEL_2").reason("No-show pattern").issuedBy("admin@cricbook.com").build());
        if (safetySuspensionRepository.count() == 0) safetySuspensionRepository.save(SafetySuspension.builder().entityType("USER").entityId(riskUser.getId()).startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(3)).status("ACTIVE").reason("Repeated no-shows").build());
        if (bannedEntityRepository.count() == 0) bannedEntityRepository.save(BannedEntity.builder().type("PHONE").value(riskUser.getPhone()).reason("Fraud risk sample").active(true).build());
        if (moderationAppealRepository.count() == 0) moderationAppealRepository.save(ModerationAppeal.builder().user(riskUser).caseId(1L).caseType("SUSPENSION").message("I want to explain the missed game.").status("PENDING").build());
        if (mediaAssetRepository.count() == 0) mediaAssetRepository.save(MediaAsset.builder().ownerType("USER").ownerId(riskUser.getId()).mediaType("PROFILE_PHOTO").imageUrl("/uploads/demo-profile.jpg").thumbnailUrl("/uploads/demo-profile-thumb.jpg").moderationStatus("PENDING").contentType("image/jpeg").fileSizeBytes(120000L).originalFilename("demo-profile.jpg").active(true).build());
        if (fraudProfileRepository.count() == 0) fraudProfileRepository.save(FraudProfile.builder().user(riskUser).riskLevel("HIGH").reason("Same device used by multiple accounts").signals("DEVICE_MATCH,REFUND_SPIKE").status("OPEN").build());
        Team team = teamRepository.findAll().stream().filter(item -> "Night Strikers".equalsIgnoreCase(item.getName())).findFirst().orElseGet(() -> teamRepository.save(Team.builder().captain(player).name("Night Strikers").sport("Box Cricket").city("Ahmedabad").description("Demo team for admin moderation").visibility(TeamVisibility.PUBLIC).maxMembers(12).build()));
        if (teamModerationRepository.count() == 0) teamModerationRepository.save(TeamModeration.builder().team(team).action("REVIEW").reason("Demo team moderation record").moderatorId("admin@cricbook.com").build());
        Review review = reviewRepository.findAll().stream().findFirst().orElseGet(() -> reviewRepository.save(Review.builder().userId(player.getId()).turfId(turf.getId()).rating(2).comment("Owner cancelled last minute.").build()));
        if (reviewModerationRepository.count() == 0) reviewModerationRepository.save(ReviewModeration.builder().reviewId(review.getId()).action("INVESTIGATE").moderatorId("admin@cricbook.com").reason("Reported review sample").build());
        if (trustAdjustmentRepository.count() == 0) trustAdjustmentRepository.save(TrustAdjustment.builder().user(riskUser).oldScore(55).newScore(45).reason("No-show penalty").adminId("admin@cricbook.com").build());
        if (communityModerationRepository.count() == 0) communityModerationRepository.save(CommunityModeration.builder().contentType("POST").contentId(9001L).action("HIDE").reason("Spam recruitment post").moderatorId("admin@cricbook.com").build());
    }

    private void seedOpsShell() {
        if (incidentRepository.count() == 0) incidentRepository.save(Incident.builder().name("Demo Razorpay Delay").severity("HIGH").status("INVESTIGATING").message("Payment gateway callback latency under review.").build());
        if (permissionAuditRepository.count() == 0) permissionAuditRepository.save(PermissionAudit.builder().actor("admin@cricbook.com").action("ROLE_ASSIGNED").entity("Admin User").details("Assigned sample support role").build());
        if (auditLogRepository.count() == 0) auditLogRepository.save(AuditLog.builder().adminId("admin@cricbook.com").action("DEMO_DATA_SEEDED").entity("AdminDemoDataSeeder").build());
    }

    private void setting(String key, String value) {
        if (platformSettingRepository.findByKey(key).isEmpty()) {
            platformSettingRepository.save(PlatformSetting.builder().key(key).value(value).build());
        }
    }
}
