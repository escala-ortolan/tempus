package com.eddyizm.tempus.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Session-wide rating overrides.
 *
 * <p>Subsonic only exposes ratings through {@code setRating}; a song object that was fetched
 * before the user rated it keeps the old {@code userRating} until it is fetched again. The
 * registry stores the decision made in this session so every screen (and the media notification /
 * Android Auto buttons) can show the new value immediately.</p>
 *
 * <p>0 is kept as a real override: it means "the rating was removed", which must win over a server
 * value that still says 1.</p>
 */
public final class RatingRegistry {

    private static final Map<String, Integer> ratings = new HashMap<>();

    /**
     * Set by the session callback so the notification / Android Auto button follows a rating made
     * anywhere in the app. It runs on the caller's thread while the lock is held, so it must only
     * post work.
     */
    public static volatile Runnable onChange;

    private RatingRegistry() {
    }

    /**
     * Records a rating and returns the value that was overridden, or {@code null} when there was no
     * override yet. The caller can pass that value to {@link #revertIfCurrent} if the server
     * refuses the write.
     */
    public static synchronized Integer set(String id, int rating) {
        if (id == null) return null;
        Integer previous = ratings.put(id, rating);
        changed();
        return previous;
    }

    public static synchronized int resolve(String id, int serverRating) {
        if (id == null) return serverRating;
        Integer rating = ratings.get(id);
        return rating != null ? rating : serverRating;
    }

    public static synchronized boolean isDisliked(String id, int serverRating) {
        return resolve(id, serverRating) == 1;
    }

    /**
     * Restores the value from before {@link #set}, but only when no newer decision replaced it
     * while the request was in flight. Returns whether the revert happened.
     */
    public static synchronized boolean revertIfCurrent(String id, int rating, Integer previous) {
        if (id == null) return false;
        if (!Objects.equals(ratings.get(id), rating)) return false;
        if (previous == null) {
            ratings.remove(id);
        } else {
            ratings.put(id, previous);
        }
        changed();
        return true;
    }

    public static synchronized void clear() {
        if (ratings.isEmpty()) return;
        ratings.clear();
        changed();
    }

    private static void changed() {
        Runnable listener = onChange;
        if (listener != null) listener.run();
    }
}
