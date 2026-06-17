package com.example.Cricbook.service;

import com.example.Cricbook.dto.SubscribeMembershipRequest;
import com.example.Cricbook.dto.UserMembershipResponse;
import com.example.Cricbook.entity.MembershipPlan;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.entity.UserMembership;
import com.example.Cricbook.enums.MembershipPlanType;
import com.example.Cricbook.enums.MembershipStatus;
import com.example.Cricbook.repository.MembershipPlanRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UserMembershipRepository;
import com.example.Cricbook.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock private MembershipPlanRepository planRepository;
    @Mock private UserMembershipRepository membershipRepository;
    @Mock private UserRepository userRepository;
    @Mock private TurfRepository turfRepository;

    @InjectMocks
    private MembershipService membershipService;

    @Test
    void subscribeActivatesUserPremiumMembership() {
        User user = User.builder().id(1L).email("u@test.com").build();
        MembershipPlan plan = MembershipPlan.builder()
                .id(2L)
                .name("Player Premium")
                .type(MembershipPlanType.USER_PREMIUM)
                .price(BigDecimal.valueOf(299))
                .durationDays(30)
                .discountPercent(10)
                .prioritySlots(true)
                .featuredListing(false)
                .build();
        SubscribeMembershipRequest request = new SubscribeMembershipRequest();
        request.setPlanId(2L);

        when(userRepository.findByEmailOrPhone("u@test.com")).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(plan));
        when(membershipRepository.save(any(UserMembership.class))).thenAnswer(invocation -> {
            UserMembership membership = invocation.getArgument(0);
            membership.setId(8L);
            return membership;
        });

        UserMembershipResponse response = membershipService.subscribe("u@test.com", request);

        assertEquals("Player Premium", response.getPlanName());
        assertEquals(MembershipStatus.ACTIVE, response.getStatus());
        assertEquals(10, response.getDiscountPercent());
    }
}
