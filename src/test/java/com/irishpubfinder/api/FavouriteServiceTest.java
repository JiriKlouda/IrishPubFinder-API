package com.irishpubfinder.api;

import com.irishpubfinder.api.dto.FavouriteRequest;
import com.irishpubfinder.api.exception.DuplicateFavouriteException;
import com.irishpubfinder.api.exception.FavouriteNotFoundException;
import com.irishpubfinder.api.model.Favourite;
import com.irishpubfinder.api.repository.FavouriteRepository;
import com.irishpubfinder.api.service.FavouriteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavouriteServiceTest {

    @Mock
    private FavouriteRepository repository;

    @InjectMocks
    private FavouriteService service;

    private static final String USER_ID = "user-123";
    private static final String PLACE_ID = "ChIJN1t_tDeuEmsRUsoyG83frY4";

    private FavouriteRequest sampleRequest() {
        return new FavouriteRequest(
            PLACE_ID,
            "The Stag's Head",
            "1 Dame Court, Dublin 2",
            53.3441,
            -6.2675,
            4.5,
            "https://maps.google.com/?cid=123",
            null
        );
    }

    @Test
    void getFavourites_returnsUserFavourites() {
        Favourite fav = Favourite.builder()
            .id(1L).userId(USER_ID).placeId(PLACE_ID)
            .name("The Stag's Head").address("1 Dame Court, Dublin 2")
            .createdAt(LocalDateTime.now()).build();

        when(repository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(fav));

        List<Favourite> result = service.getFavourites(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("The Stag's Head");
    }

    @Test
    void addFavourite_savesAndReturns() {
        when(repository.existsByUserIdAndPlaceId(USER_ID, PLACE_ID)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            Favourite f = inv.getArgument(0);
            f.setId(1L);
            f.setCreatedAt(LocalDateTime.now());
            return f;
        });

        Favourite result = service.addFavourite(USER_ID, sampleRequest());

        assertThat(result.getName()).isEqualTo("The Stag's Head");
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        verify(repository).save(any(Favourite.class));
    }

    @Test
    void addFavourite_throwsOnDuplicate() {
        when(repository.existsByUserIdAndPlaceId(USER_ID, PLACE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.addFavourite(USER_ID, sampleRequest()))
            .isInstanceOf(DuplicateFavouriteException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void removeFavourite_deletesWhenExists() {
        when(repository.existsByUserIdAndPlaceId(USER_ID, PLACE_ID)).thenReturn(true);

        service.removeFavourite(USER_ID, PLACE_ID);

        verify(repository).deleteByUserIdAndPlaceId(USER_ID, PLACE_ID);
    }

    @Test
    void removeFavourite_throwsWhenNotFound() {
        when(repository.existsByUserIdAndPlaceId(USER_ID, PLACE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.removeFavourite(USER_ID, PLACE_ID))
            .isInstanceOf(FavouriteNotFoundException.class);
    }

    @Test
    void isFavourite_returnsCorrectState() {
        when(repository.existsByUserIdAndPlaceId(USER_ID, PLACE_ID)).thenReturn(true);
        assertThat(service.isFavourite(USER_ID, PLACE_ID)).isTrue();

        when(repository.existsByUserIdAndPlaceId(USER_ID, PLACE_ID)).thenReturn(false);
        assertThat(service.isFavourite(USER_ID, PLACE_ID)).isFalse();
    }
}
