package com.netherpyro.glcv.baker

/**
 * @author mmikhailov on 29.04.2020.
 */
class MuteCalculator {

    private val muteAnswers = HashMap<String, Boolean>()

    /**
     * @return whether should mute entry
     */
    fun addEntry(tag: String, mutePreference: Boolean, hasAudio: Boolean): Boolean =
            (mutePreference or hasAudio.not()).also { muteAnswers[tag] = it }

    fun shouldSound(tag: String) = muteAnswers[tag]?.not() ?: false

    fun shouldSoundAtLeastOne(): Boolean = muteAnswers.values.fold(false) { acc, answer -> acc or !answer }
}