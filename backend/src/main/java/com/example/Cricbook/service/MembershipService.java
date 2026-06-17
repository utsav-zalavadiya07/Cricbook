package com.example.Cricbook.service;

import com.example.Cricbook.dto.MembershipPlanResponse;
import com.example.Cricbook.dto.SubscribeMembershipRequest;
import com.example.Cricbook.dto.UserMembershipResponse;
import com.example.Cricbook.entity.MembershipPlan;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.entity.UserMembership;
import com.example.Cricbook.enums.MembershipPlanType;
import com.example.Cricbook.enums.MembershipStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.MembershipPlanRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UserMembershipRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipPlanRepository planRepository;
    private final UserMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final TurfRepository turfRepository;

    @Transactional
    public List<MembershipPlanResponse> getPlans() {
        ensureDefaultPlans();
        return planRepository.findAllByOrderByPriceAsc().stream().map(this::mapPlan).toList();
    }

    @Transactional
    public MembershipPlanResponse createPlan(MembershipPlan plan) {
        if (plan.getName() == null || plan.getName().isBlank()) {
            throw new ApiException("Plan name is required");
        }
        if (plan.getPrice() == null) plan.setPrice(BigDecimal.ZERO);
        if (plan.getDurationDays() == null || plan.getDurationDays() <= 0) plan.setDurationDays(30);
        if (plan.getDiscountPercent() == null) plan.setDiscountPercent(0);
        if (plan.getPrioritySlots() == null) plan.setPrioritySlots(false);
        if (plan.getFeaturedListing() == null) plan.setFeaturedListing(false);
        return mapPlan(planRepository.save(plan));
    }

    @Transactional
    public UserMembershipResponse subscribe(String username, SubscribeMembershipRequest request) {
        if (request == null || request.getPlanId() == null) {
            throw new ApiException("Plan is required");
        }
        User user = findUser(username);
        MembershipPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ApiException("Plan not found"));
        Turf turf = null;
        if (plan.getType() == MembershipPlanType.OWNER_FEATURED) {
            if (request.getTurfId() == null) {
                throw new ApiException("Turf is required for owner featured plans");
            }
            turf = turfRepository.findById(request.getTurfId())
                    .orElseThrow(() -> new ApiException("Turf not found"));
            if (!turf.getOwner().getId().equals(user.getId())) {
                throw new ApiException("You can promote only your own turf");
            }
        }

        LocalDateTime startsAt = LocalDateTime.now();
        LocalDateTime endsAt = startsAt.plusDays(plan.getDurationDays());
        UserMembership membership = membershipRepository.save(UserMembership.builder()
                .user(user)
                .plan(plan)
                .turf(turf)
                .status(MembershipStatus.ACTIVE)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build());

        if (turf != null && Boolean.TRUE.equals(plan.getFeaturedListing())) {
            turf.setFeaturedUntil(endsAt);
            turfRepository.save(turf);
        }
        return mapMembership(membership);
    }

    @Transactional(readOnly = true)
    public List<UserMembershipResponse> getMyMemberships(String username) {
        User user = findUser(username);
        return membershipRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::mapMembership).toList();
    }

    @Transactional(readOnly = true)
    public UserMembershipResponse getActiveMembership(String username) {
        User user = findUser(username);
        return membershipRepository.findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(user.getId(), MembershipStatus.ACTIVE, LocalDateTime.now())
                .map(this::mapMembership)
                .orElse(null);
    }

    private MembershipPlanResponse mapPlan(MembershipPlan plan) {
        return MembershipPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .type(plan.getType())
                .price(plan.getPrice())
                .durationDays(plan.getDurationDays())
                .discountPercent(plan.getDiscountPercent())
                .prioritySlots(plan.getPrioritySlots())
                .featuredListing(plan.getFeaturedListing())
                .description(plan.getDescription())
                .build();
    }

    private UserMembershipResponse mapMembership(UserMembership membership) {
        return UserMembershipResponse.builder()
                .id(membership.getId())
                .planId(membership.getPlan().getId())
                .planName(membership.getPlan().getName())
                .type(membership.getPlan().getType())
                .turfId(membership.getTurf() == null ? null : membership.getTurf().getId())
                .turfName(membership.getTurf() == null ? null : membership.getTurf().getName())
                .discountPercent(membership.getPlan().getDiscountPercent())
                .prioritySlots(membership.getPlan().getPrioritySlots())
                .featuredListing(membership.getPlan().getFeaturedListing())
                .status(membership.getStatus())
                .startsAt(membership.getStartsAt())
                .endsAt(membership.getEndsAt())
                .build();
    }

    private User findUser(String loginId) {
        return userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> new ApiException("User not found"));
    }

    private void ensureDefaultPlans() {
        if (!planRepository.findAll().isEmpty()) {
            return;
        }
        planRepository.save(MembershipPlan.builder()
                .name("Player Premium")
                .type(MembershipPlanType.USER_PREMIUM)
                .price(BigDecimal.valueOf(299))
                .durationDays(30)
                .discountPercent(10)
                .prioritySlots(true)
                .featuredListing(false)
                .description("Discounted bookings and priority slot access.")
                .build());
        planRepository.save(MembershipPlan.builder()
                .name("Owner Featured")
                .type(MembershipPlanType.OWNER_FEATURED)
                .price(BigDecimal.valueOf(999))
                .durationDays(30)
                .discountPercent(0)
                .prioritySlots(false)
                .featuredListing(true)
                .description("Promoted turf listing for owners.")
                .build());
    }
}
