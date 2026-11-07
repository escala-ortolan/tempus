package com.eddyizm.tempus.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RatingRegistryTest {

    @Before
    public void reset() {
        RatingRegistry.onChange = null;
        RatingRegistry.clear();
    }

    @After
    public void dropTheListener() {
        RatingRegistry.onChange = null;
    }

    @Test
    public void untouchedItemFollowsTheServer() {
        assertEquals(3, RatingRegistry.resolve("1", 3));
        assertFalse(RatingRegistry.isDisliked("1", 3));
    }

    @Test
    public void aDislikeWinsOverAServerThatHasNoRating() {
        RatingRegistry.set("1", 1);

        assertEquals(1, RatingRegistry.resolve("1", 0));
        assertTrue(RatingRegistry.isDisliked("1", 0));
    }

    @Test
    public void removingADislikeWinsOverAServerThatStillSaysOne() {
        RatingRegistry.set("1", 0);

        assertEquals(0, RatingRegistry.resolve("1", 1));
        assertFalse(RatingRegistry.isDisliked("1", 1));
    }

    @Test
    public void onlyTheItemThatWasTouchedIsAffected() {
        RatingRegistry.set("1", 1);

        assertEquals(4, RatingRegistry.resolve("2", 4));
    }

    @Test
    public void aFailedWriteRestoresTheEarlierValue() {
        RatingRegistry.set("1", 1);
        Integer previous = RatingRegistry.set("1", 0);

        assertTrue(RatingRegistry.revertIfCurrent("1", 0, previous));
        assertEquals(1, RatingRegistry.resolve("1", 0));
    }

    @Test
    public void aFailedWriteDoesNotUndoANewerDecision() {
        Integer previous = RatingRegistry.set("1", 1);
        RatingRegistry.set("1", 5);

        assertFalse(RatingRegistry.revertIfCurrent("1", 1, previous));
        assertEquals(5, RatingRegistry.resolve("1", 0));
    }

    @Test
    public void aFailedFirstWriteFallsBackToTheServer() {
        Integer previous = RatingRegistry.set("1", 1);

        assertTrue(RatingRegistry.revertIfCurrent("1", 1, previous));
        assertEquals(2, RatingRegistry.resolve("1", 2));
    }

    @Test
    public void everyMutationTellsTheListener() {
        int[] fired = {0};
        RatingRegistry.onChange = () -> fired[0]++;

        Integer previous = RatingRegistry.set("1", 1);
        assertEquals(1, fired[0]);

        RatingRegistry.revertIfCurrent("1", 1, previous);
        assertEquals(2, fired[0]);

        RatingRegistry.set("1", 1);
        assertEquals(3, fired[0]);

        RatingRegistry.clear();
        assertEquals(4, fired[0]);
    }

    @Test
    public void aNullIdStaysQuiet() {
        int[] fired = {0};
        RatingRegistry.onChange = () -> fired[0]++;

        RatingRegistry.set(null, 1);

        assertEquals(0, fired[0]);
        assertEquals(2, RatingRegistry.resolve(null, 2));
    }
}
