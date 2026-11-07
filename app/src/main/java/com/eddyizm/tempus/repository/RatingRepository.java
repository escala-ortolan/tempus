package com.eddyizm.tempus.repository;

import androidx.annotation.NonNull;

import com.eddyizm.tempus.App;
import com.eddyizm.tempus.interfaces.RatingCallback;
import com.eddyizm.tempus.subsonic.base.ApiResponse;
import com.eddyizm.tempus.subsonic.models.ResponseStatus;
import com.eddyizm.tempus.util.RatingRegistry;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Ratings are the portable "dislike" flag: Subsonic has no thumbs-down, but {@code setRating(1)}
 * is understood by Navidrome and by every other Subsonic server. A rating of 1 therefore means
 * "disliked"; 0 removes the rating.
 */
public class RatingRepository {

    public static final int DISLIKE = 1;
    public static final int NO_RATING = 0;

    // Subsonic reports a refusal as a 200 whose body says failed, so the status line alone cannot
    // tell a stored rating from a rejected one.
    private static boolean serverRefused(Response<ApiResponse> response) {
        return response.body() != null
                && response.body().getSubsonicResponse() != null
                && ResponseStatus.FAILED.equals(response.body().getSubsonicResponse().getStatus());
    }

    public void setRating(String songId, int rating, RatingCallback callback) {
        if (songId == null) {
            if (callback != null) callback.onError();
            return;
        }

        // Publish the decision before the request so every surface can react immediately.
        Integer previous = RatingRegistry.set(songId, rating);

        App.getSubsonicClientInstance(false)
                .getMediaAnnotationClient()
                .setRating(songId, rating)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                        if (response.isSuccessful() && !serverRefused(response)) {
                            if (callback != null) callback.onSuccess();
                        } else {
                            RatingRegistry.revertIfCurrent(songId, rating, previous);
                            if (callback != null) callback.onError();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                        RatingRegistry.revertIfCurrent(songId, rating, previous);
                        if (callback != null) callback.onError();
                    }
                });
    }

    public void toggleDislike(String songId, int currentRating, RatingCallback callback) {
        setRating(songId, currentRating == DISLIKE ? NO_RATING : DISLIKE, callback);
    }
}
