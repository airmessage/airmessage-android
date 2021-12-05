package me.tagavari.airmessage.connection.exception

import java.lang.Exception

class AMRemoteUpdateException @JvmOverloads constructor(
    val errorCode: Int,
    val errorDetails: String? = null
): Exception() {
    companion object {
        const val errorCodeMismatch = 0
    }
}