package com.eddyizm.tempus.interfaces;

import androidx.annotation.Keep;

@Keep
public interface RatingCallback {
    default void onSuccess() {}

    default void onError() {}
}
