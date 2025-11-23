package com.aryanspatel.moodmatch.domain.realtime

sealed class OutgoingCommand{
    data class SendMessage(val roomId: String, val text: String): OutgoingCommand()
    data class SetTyping(val roomId: String, val isTyping: Boolean): OutgoingCommand()
    data object SendHeartbeat : OutgoingCommand()
    data class JoinQueue(val moodLabel: String) : OutgoingCommand()
    data object LeaveQueue: OutgoingCommand()
    data class LeaveRoom(val roomId: String): OutgoingCommand()
}
