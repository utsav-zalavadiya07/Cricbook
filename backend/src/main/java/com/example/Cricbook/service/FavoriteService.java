package com.example.Cricbook.service;

import com.example.Cricbook.dto.TurfResponse;
import com.example.Cricbook.entity.FavoriteTurf;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.TurfStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.mapper.TurfMapper;
import com.example.Cricbook.repository.FavoriteTurfRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteTurfRepository favoriteRepository;
    private final UserRepository userRepository;
    private final TurfRepository turfRepository;
    private final TurfMapper turfMapper;

    @Transactional
    public TurfResponse saveFavorite(String username, Long turfId) {
        User user = findUser(username);
        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new ApiException("Turf not found"));
        if (turf.getStatus() != TurfStatus.ACTIVE) {
            throw ApiException.badRequest("Only active turfs can be added to favorites.");
        }

        favoriteRepository.findByUserIdAndTurfId(user.getId(), turfId)
                .orElseGet(() -> favoriteRepository.save(
                        FavoriteTurf.builder().user(user).turf(turf).build()
                ));

        return turfMapper.toTurfResponse(turf);
    }

    @Transactional(readOnly = true)
    public List<TurfResponse> getFavorites(String username) {
        User user = findUser(username);
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(FavoriteTurf::getTurf)
                .filter(turf -> turf.getStatus() == TurfStatus.ACTIVE)
                .map(turfMapper::toTurfResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeFavorite(String username, Long turfId) {
        User user = findUser(username);
        favoriteRepository.deleteByUserIdAndTurfId(user.getId(), turfId);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(String username, Long turfId) {
        User user = findUser(username);
        return favoriteRepository.existsByUserIdAndTurfId(user.getId(), turfId);
    }

    private User findUser(String username) {
        return userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> new ApiException("User not found"));
    }
}
