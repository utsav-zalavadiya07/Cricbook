package com.example.Cricbook.service;

import com.example.Cricbook.dto.AddMoneyRequest;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyService {

    private final LoyaltyPointRepository loyaltyPointRepository;
    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;

    @Transactional
    public LoyaltyPoint getOrCreateLoyaltyRecord(User user) {
        return loyaltyPointRepository.findByUserId(user.getId())
                .orElseGet(() -> loyaltyPointRepository.save(LoyaltyPoint.builder()
                        .user(user)
                        .points(0)
                        .history("[]")
                        .build()));
    }

    @Transactional
    public void earnPoints(User user, int amount, String reason) {
        LoyaltyPoint record = getOrCreateLoyaltyRecord(user);
        record.setPoints(record.getPoints() + amount);

        String logMsg = String.format("[%s] Earned %d points for: %s\n", LocalDateTime.now(), amount, reason);
        record.setHistory(record.getHistory() + logMsg);
        loyaltyPointRepository.save(record);

        notificationService.pushNotification(
                user.getId(),
                user.getEmail(),
                "Loyalty Points Credited",
                String.format("You earned %d loyalty points! Reason: %s", amount, reason)
        );
    }

    @Transactional
    public void redeemPoints(String username, int pointsToRedeem) {
        if (pointsToRedeem <= 0 || pointsToRedeem % 10 != 0) {
            throw new ApiException("Redemption amount must be positive and in multiples of 10 points.");
        }

        User user = userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> new ApiException("User not found"));

        LoyaltyPoint record = getOrCreateLoyaltyRecord(user);
        if (record.getPoints() < pointsToRedeem) {
            throw new ApiException("Insufficient loyalty points balance.");
        }

        record.setPoints(record.getPoints() - pointsToRedeem);
        String logMsg = String.format("[%s] Redeemed %d points for wallet cash\n", LocalDateTime.now(), pointsToRedeem);
        record.setHistory(record.getHistory() + logMsg);
        loyaltyPointRepository.save(record);

        // Rs 1 per 10 points
        BigDecimal cashVal = BigDecimal.valueOf(pointsToRedeem / 10);
        walletService.addMoney(username, new AddMoneyRequest(cashVal));

        notificationService.pushNotification(
                user.getId(),
                user.getEmail(),
                "Points Redeemed",
                String.format("Successfully redeemed %d points for Rs %s in your wallet!", pointsToRedeem, cashVal)
        );
    }

    @Transactional(readOnly = true)
    public Integer getPointsBalance(String username) {
        User user = findUser(username);
        return loyaltyPointRepository.findByUserId(user.getId())
                .map(LoyaltyPoint::getPoints)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public String getPointsHistory(String username) {
        User user = findUser(username);
        return loyaltyPointRepository.findByUserId(user.getId())
                .map(LoyaltyPoint::getHistory)
                .filter(history -> history != null && !history.isBlank())
                .orElse("");
    }

    @Transactional(readOnly = true)
    public String generateReferralCode(String username) {
        User user = findUser(username);
        return "CRIC-" + user.getName().toUpperCase().replaceAll("\\s+", "") + "-" + user.getId();
    }

    @Transactional
    public void claimReferral(String username, String code) {
        User invitee = userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> new ApiException("Invitee user not found"));

        // Check if invitee already registered a referral
        Optional<Referral> existing = referralRepository.findByInviteeId(invitee.getId());
        if (existing.isPresent()) {
            throw new ApiException("You have already claimed a referral invite.");
        }

        // Find referrer by matching code
        String[] parts = code.split("-");
        if (parts.length < 3) {
            throw new ApiException("Invalid referral code format.");
        }
        
        Long referrerId;
        try {
            referrerId = Long.parseLong(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            throw new ApiException("Invalid referral code signature.");
        }

        User referrer = userRepository.findById(referrerId)
                .orElseThrow(() -> new ApiException("Referrer account not found."));

        if (referrer.getId().equals(invitee.getId())) {
            throw new ApiException("You cannot refer yourself.");
        }

        referralRepository.save(Referral.builder()
                .referrer(referrer)
                .invitee(invitee)
                .referralCode(code)
                .status("PENDING")
                .build());

        log.info("Linked referral: Referrer {} -> Invitee {}", referrer.getEmail(), invitee.getEmail());
    }

    @Transactional
    public void rewardReferralIfPending(User invitee) {
        Optional<Referral> referralOpt = referralRepository.findByInviteeId(invitee.getId());
        if (referralOpt.isPresent() && "PENDING".equals(referralOpt.get().getStatus())) {
            Referral referral = referralOpt.get();
            referral.setStatus("COMPLETED");
            referralRepository.save(referral);

            // Reward referrer with 100 points
            earnPoints(referral.getReferrer(), 100, "Successful referral of " + invitee.getName());
            
            // Reward invitee with 50 points
            earnPoints(invitee, 50, "Claimed referral code: " + referral.getReferralCode());

            log.info("Rewarded completed referral: Referrer {} (+100 pts) | Invitee {} (+50 pts)",
                    referral.getReferrer().getEmail(), invitee.getEmail());
        }
    }

    @Transactional(readOnly = true)
    public List<Referral> getMyReferrals(String username) {
        User user = findUser(username);
        return referralRepository.findByReferrerId(user.getId());
    }

    private User findUser(String username) {
        return userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Your account could not be loaded. Please sign in again."));
    }
}
