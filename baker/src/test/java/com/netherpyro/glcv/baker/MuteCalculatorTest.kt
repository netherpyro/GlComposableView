package com.netherpyro.glcv.baker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MuteCalculatorTest {

    @Test
    fun test_muteCalculatorAnswers() {
        val muteCalculator = MuteCalculator()

        // GIVEN file1 WHEN user wants mute AND file has audio THEN audio track should mute
        assertTrue(muteCalculator.shouldMute(mutePreference = true, hasAudio = true))

        // GIVEN file2 WHEN user wants mute AND file hasn't audio THEN audio track should mute
        assertTrue(muteCalculator.shouldMute(mutePreference = true, hasAudio = false))

        // GIVEN file3 WHEN user don't want mute AND file hasn't audio THEN audio track should mute
        assertTrue(muteCalculator.shouldMute(mutePreference = false, hasAudio = false))

        // GIVEN file4 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.shouldMute(mutePreference = false, hasAudio = true))
    }

    @Test
    fun test_muteCalculatorResultAllPositiveAnswers() {
        val muteCalculator = MuteCalculator()

        // GIVEN file1 WHEN user wants mute AND file has audio THEN audio track should mute
        assertTrue(muteCalculator.shouldMute(mutePreference = true, hasAudio = true))

        // GIVEN file2 WHEN user wants mute AND file hasn't audio THEN audio track should mute
        assertTrue(muteCalculator.shouldMute(mutePreference = true, hasAudio = false))

        // GIVEN file3 WHEN user don't want mute AND file hasn't audio THEN audio track should mute
        assertTrue(muteCalculator.shouldMute(mutePreference = false, hasAudio = false))

        // GIVEN file4 WHEN user don't want mute AND file hasn't audio THEN audio track should mute
        assertTrue(muteCalculator.shouldMute(mutePreference = false, hasAudio = false))

        // GIVEN mute answers WHEN there is all positive answers THEN claim that audio is not present
        assertFalse(muteCalculator.hasAudio())
    }

    @Test
    fun test_muteCalculatorResultAllNegativeAnswers() {
        val muteCalculator = MuteCalculator()

        // GIVEN file1 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.shouldMute(mutePreference = false, hasAudio = true))

        // GIVEN file2 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.shouldMute(mutePreference = false, hasAudio = true))

        // GIVEN file3 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.shouldMute(mutePreference = false, hasAudio = true))

        // GIVEN file4 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.shouldMute(mutePreference = false, hasAudio = true))

        // GIVEN mute answers WHEN there is all negative answers THEN claim that audio is present
        assertTrue(muteCalculator.hasAudio())
    }

    @Test
    fun test_muteCalculatorResultDifferentAnswers() {
        val muteCalculator = MuteCalculator()

        // GIVEN file1 WHEN user wants mute AND file has audio THEN audio track should mute
        assertTrue(muteCalculator.shouldMute(mutePreference = true, hasAudio = true))

        // GIVEN file2 WHEN user wants mute AND file hasn't audio THEN audio track should mute
        assertTrue(muteCalculator.shouldMute(mutePreference = true, hasAudio = false))

        // GIVEN file3 WHEN user don't want mute AND file hasn't audio THEN audio track should mute
        assertTrue(muteCalculator.shouldMute(mutePreference = false, hasAudio = false))

        // GIVEN file4 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.shouldMute(mutePreference = false, hasAudio = true))

        // GIVEN file5 WHEN user wants mute AND file has audio THEN audio track should mute
        assertTrue(muteCalculator.shouldMute(mutePreference = true, hasAudio = true))

        // GIVEN file6 WHEN user wants mute AND file has audio THEN audio track should mute
        assertTrue(muteCalculator.shouldMute(mutePreference = true, hasAudio = true))

        // GIVEN file7 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.shouldMute(mutePreference = false, hasAudio = true))

        // GIVEN mute answers WHEN there is at least one negative 'mute' answer THEN claim that audio is present
        assertTrue(muteCalculator.hasAudio())
    }
}
