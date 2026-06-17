package com.example.Cricbook.service;

import com.example.Cricbook.entity.User;
import com.example.Cricbook.entity.PlayerProfile;
import com.example.Cricbook.entity.UserSegment;
import com.example.Cricbook.enums.MembershipStatus;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SegmentationService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final UserSegmentRepository segmentRepository;

    public List<User> resolveSegmentUsers(String segmentName) {
        log.info("Resolving users for segment: {}", segmentName);
        List<User> allUsers = userRepository.findAll();

        // Handle standard segments by name
        if (segmentName.equalsIgnoreCase("New Users")) {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            return allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(sevenDaysAgo))
                    .collect(Collectors.toList());
        } else if (segmentName.equalsIgnoreCase("Premium")) {
            LocalDateTime now = LocalDateTime.now();
            return allUsers.stream()
                    .filter(u -> userMembershipRepository.findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(
                            u.getId(), MembershipStatus.ACTIVE, now).isPresent())
                    .collect(Collectors.toList());
        } else if (segmentName.equalsIgnoreCase("Inactive")) {
            return allUsers.stream()
                    .filter(u -> bookingRepository.findByUserId(u.getId()).isEmpty())
                    .collect(Collectors.toList());
        } else if (segmentName.equalsIgnoreCase("Owners")) {
            return allUsers.stream()
                    .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_OWNER))
                    .collect(Collectors.toList());
        } else if (segmentName.equalsIgnoreCase("Frequent Players")) {
            return allUsers.stream()
                    .filter(u -> bookingRepository.findByUserId(u.getId()).size() >= 5)
                    .collect(Collectors.toList());
        }

        // Custom segment from DB rules
        Optional<UserSegment> segmentOpt = segmentRepository.findByName(segmentName);
        if (segmentOpt.isPresent()) {
            UserSegment segment = segmentOpt.get();
            String ruleType = segment.getRuleType();
            String ruleValue = segment.getRuleValue();

            if ("BOOKINGS_COUNT".equalsIgnoreCase(ruleType)) {
                int countThreshold = Integer.parseInt(ruleValue);
                return allUsers.stream()
                        .filter(u -> bookingRepository.findByUserId(u.getId()).size() >= countThreshold)
                        .collect(Collectors.toList());
            } else if ("CITY".equalsIgnoreCase(ruleType)) {
                return allUsers.stream()
                        .filter(u -> {
                            Optional<PlayerProfile> profile = playerProfileRepository.findByUserId(u.getId());
                            return profile.isPresent() && ruleValue.equalsIgnoreCase(profile.get().getCity());
                        })
                        .collect(Collectors.toList());
            } else if ("INACTIVITY".equalsIgnoreCase(ruleType)) {
                int daysThreshold = Integer.parseInt(ruleValue);
                LocalDateTime cutoff = LocalDateTime.now().minusDays(daysThreshold);
                return allUsers.stream()
                        .filter(u -> {
                            var bookings = bookingRepository.findByUserId(u.getId());
                            if (bookings.isEmpty()) return true;
                            // Check if no bookings are after cutoff
                            return bookings.stream().noneMatch(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(cutoff));
                        })
                        .collect(Collectors.toList());
            }
        }

        log.warn("Segment rule '{}' could not be evaluated; returning all active users as fallback", segmentName);
        return allUsers;
    }
}
