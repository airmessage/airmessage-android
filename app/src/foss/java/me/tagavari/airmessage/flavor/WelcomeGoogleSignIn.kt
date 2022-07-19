package me.tagavari.airmessage.flavor

import me.tagavari.airmessage.fragment.FragmentOnboardingWelcome

class WelcomeGoogleSignIn constructor(private val fragment: FragmentOnboardingWelcome) {
    val isSupported = false
    fun launch() = Unit
}