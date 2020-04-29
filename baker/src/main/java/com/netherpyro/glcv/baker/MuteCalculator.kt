package com.netherpyro.glcv.baker

/**
 * @author mmikhailov on 29.04.2020.
 */
class MuteCalculator {

    private val muteAnswers = ArrayList<Boolean>()

    fun shouldMute(mutePreference: Boolean, hasAudio: Boolean): Boolean =
            (mutePreference or hasAudio.not()).also { muteAnswers.add(it) }

    fun hasAudio(): Boolean = muteAnswers.fold(false) { acc, answer -> acc or !answer }
}