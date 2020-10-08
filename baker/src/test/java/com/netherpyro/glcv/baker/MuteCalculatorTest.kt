package com.netherpyro.glcv.baker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MuteCalculatorTest {

    @Test
    fun test_muteCalculatorAnswers() {
        val muteCalculator = MuteCalculator()

        // GIVEN file1 WHEN user wants mute AND file has audio THEN audio track should mute
        assertTrue(muteCalculator.addEntry(tag = "file1", mutePreference = true, hasAudio = true))

        // GIVEN file2 WHEN user wants mute AND file hasn't audio THEN audio track should mute
        assertTrue(muteCalculator.addEntry(tag = "file2", mutePreference = true, hasAudio = false))

        // GIVEN file3 WHEN user don't want mute AND file hasn't audio THEN audio track should mute
        assertTrue(muteCalculator.addEntry(tag = "file3", mutePreference = false, hasAudio = false))

        // GIVEN file4 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.addEntry(tag = "file4", mutePreference = false, hasAudio = true))

        // GIVEN mute answers WHEN there is at least one negative 'mute' answer THEN claim that audio is present
        assertTrue(muteCalculator.shouldSoundAtLeastOne())
    }

    @Test
    fun test_muteCalculatorResultAllPositiveAnswers() {
        val muteCalculator = MuteCalculator()

        // GIVEN file1 WHEN user wants mute AND file has audio THEN audio track should mute
        assertTrue(muteCalculator.addEntry(tag = "file1", mutePreference = true, hasAudio = true))

        // GIVEN file2 WHEN user wants mute AND file hasn't audio THEN audio track should mute
        assertTrue(muteCalculator.addEntry(tag = "file2", mutePreference = true, hasAudio = false))

        // GIVEN file3 WHEN user don't want mute AND file hasn't audio THEN audio track should mute
        assertTrue(muteCalculator.addEntry(tag = "file3", mutePreference = false, hasAudio = false))

        // GIVEN file4 WHEN user don't want mute AND file hasn't audio THEN audio track should mute
        assertTrue(muteCalculator.addEntry(tag = "file4", mutePreference = false, hasAudio = false))

        // GIVEN mute answers WHEN there is all positive answers THEN claim that audio is not present
        assertFalse(muteCalculator.shouldSoundAtLeastOne())
    }

    @Test
    fun test_muteCalculatorResultAllNegativeAnswers() {
        val muteCalculator = MuteCalculator()

        // GIVEN file1 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.addEntry(tag = "file1", mutePreference = false, hasAudio = true))

        // GIVEN file2 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.addEntry(tag = "file2", mutePreference = false, hasAudio = true))

        // GIVEN file3 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.addEntry(tag = "file3", mutePreference = false, hasAudio = true))

        // GIVEN file4 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.addEntry(tag = "file4", mutePreference = false, hasAudio = true))

        // GIVEN mute answers WHEN there is all negative answers THEN claim that audio is present
        assertTrue(muteCalculator.shouldSoundAtLeastOne())
    }

    @Test
    fun test_muteCalculatorResultDifferentAnswers() {
        val muteCalculator = MuteCalculator()

        // GIVEN file1 WHEN user wants mute AND file has audio THEN audio track should mute
        assertTrue(muteCalculator.addEntry(tag = "file1", mutePreference = true, hasAudio = true))

        // GIVEN file2 WHEN user wants mute AND file hasn't audio THEN audio track should mute
        assertTrue(muteCalculator.addEntry(tag = "file2", mutePreference = true, hasAudio = false))

        // GIVEN file3 WHEN user don't want mute AND file hasn't audio THEN audio track should mute
        assertTrue(muteCalculator.addEntry(tag = "file3", mutePreference = false, hasAudio = false))

        // GIVEN file4 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.addEntry(tag = "file4", mutePreference = false, hasAudio = true))

        // GIVEN file5 WHEN user wants mute AND file has audio THEN audio track should mute
        assertTrue(muteCalculator.addEntry(tag = "file5", mutePreference = true, hasAudio = true))

        // GIVEN file6 WHEN user wants mute AND file has audio THEN audio track should mute
        assertTrue(muteCalculator.addEntry(tag = "file6", mutePreference = true, hasAudio = true))

        // GIVEN file7 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.addEntry(tag = "file7", mutePreference = false, hasAudio = true))

        // GIVEN mute answers WHEN there is at least one negative 'mute' answer THEN claim that audio is present
        assertTrue(muteCalculator.shouldSoundAtLeastOne())
    }

    @Test
    fun test_muteCalculatorSameFile() {
        val muteCalculator = MuteCalculator()

        // GIVEN file1 WHEN user wants mute AND file has audio THEN audio track should mute
        assertTrue(muteCalculator.addEntry(tag = "file1", mutePreference = true, hasAudio = true))

        // GIVEN file1 WHEN user wants mute AND file has audio THEN audio track should mute
        assertTrue(muteCalculator.addEntry(tag = "file1_1", mutePreference = true, hasAudio = true))

        // GIVEN file1 WHEN user don't want mute AND file has audio THEN audio track shouldn't mute
        assertFalse(muteCalculator.addEntry(tag = "file1_2", mutePreference = false, hasAudio = true))

        // GIVEN mute answers WHEN there is at least one negative 'mute' answer THEN claim that audio is present
        assertTrue(muteCalculator.shouldSoundAtLeastOne())
    }
}
