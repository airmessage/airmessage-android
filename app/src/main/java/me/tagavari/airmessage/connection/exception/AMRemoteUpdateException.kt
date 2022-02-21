package me.tagavari.airmessage.connection.exception

import java.lang.Exception

class AMRemoteUpdateException @JvmOverloads constructor(
    val errorCode: Int,
    val errorDetails: String? = null
): Exception() {
    companion object {
        const val errorCodeUnknown = 0
        const val errorCodeMismatch = 1
        const val errorCodeDownload = 2
        const val errorCodeBadPackage = 3
        const val errorCodeInternal = 4
    }
}