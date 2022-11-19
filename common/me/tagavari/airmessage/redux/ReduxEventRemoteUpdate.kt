package me.tagavari.airmessage.redux

import me.tagavari.airmessage.connection.exception.AMRemoteUpdateException

sealed class ReduxEventRemoteUpdate {
    object Initiate : ReduxEventRemoteUpdate()
    class Error(val exception: AMRemoteUpdateException) : ReduxEventRemoteUpdate()
}