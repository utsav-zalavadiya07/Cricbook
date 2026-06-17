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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteTurfRepository favoriteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TurfRepository turfRepository;
    @Mock
    private TurfMapper turfMapper;

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    void saveFavoriteRejectsInactiveTurfs() {
        User user = User.builder().id(1L).email("player@example.com").build();
        Turf inactiveTurf = Turf.builder().id(10L).name("Closed Turf").status(TurfStatus.INACTIVE).build();

        when(userRepository.findByEmailOrPhone("player@example.com")).thenReturn(Optional.of(user));
        when(turfRepository.findById(10L)).thenReturn(Optional.of(inactiveTurf));

        ApiException exception = assertThrows(ApiException.class, () -> favoriteService.saveFavorite("player@example.com", 10L));

        assertEquals("Only active turfs can be added to favorites.", exception.getMessage());
        verify(favoriteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getFavoritesReturnsActiveTurfsOnly() {
        User user = User.builder().id(1L).email("player@example.com").build();
        Turf activeTurf = Turf.builder().id(10L).name("Active Turf").status(TurfStatus.ACTIVE).build();
        Turf inactiveTurf = Turf.builder().id(11L).name("Inactive Turf").status(TurfStatus.INACTIVE).build();
        TurfResponse response = TurfResponse.builder().id(10L).name("Active Turf").status(TurfStatus.ACTIVE).build();

        when(userRepository.findByEmailOrPhone("player@example.com")).thenReturn(Optional.of(user));
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(
                FavoriteTurf.builder().user(user).turf(activeTurf).build(),
                FavoriteTurf.builder().user(user).turf(inactiveTurf).build()
        ));
        when(turfMapper.toTurfResponse(activeTurf)).thenReturn(response);

        List<TurfResponse> favorites = favoriteService.getFavorites("player@example.com");

        assertEquals(1, favorites.size());
        assertEquals("Active Turf", favorites.get(0).getName());
    }
}
