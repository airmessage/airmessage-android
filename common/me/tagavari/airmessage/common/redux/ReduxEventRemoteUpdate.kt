package me.tagavari.airmessage.common.redux

import me.tagavari.airmessage.common.connection.exception.AMRemoteUpdateException

sealed class ReduxEventRemoteUpdate {
    object Initiate : ReduxEventRemoteUpdate()
    class Error(val exception: AMRemoteUpdateException) : ReduxEventRemoteUpdate()
}