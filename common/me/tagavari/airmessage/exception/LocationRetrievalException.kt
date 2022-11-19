package me.tagavari.airmessage.exception

/**
 * The user has location services disabled
 */
class LocationDisabledException : Exception("Location services are disabled")

/**
 * The user's current location is unavailable
 */
class LocationUnavailableException : Exception("Location is currently unavailable")
