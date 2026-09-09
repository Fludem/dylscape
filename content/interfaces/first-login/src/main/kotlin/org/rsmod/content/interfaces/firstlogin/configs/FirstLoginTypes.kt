package org.rsmod.content.interfaces.firstlogin.configs

import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.timer.TimerReferences

typealias first_login_queues = FirstLoginQueues

typealias first_login_timers = FirstLoginTimers

object FirstLoginQueues : QueueReferences() {
    /**
     * Runs the chooser a tick after login. The delay matters: at `SessionStateEvent.Login` the
     * client has not been sent its vars or invs yet, so opening the panel in the login handler
     * itself would race the gameframe.
     */
    val setup = find("account_setup")
}

object FirstLoginTimers : TimerReferences() {
    /**
     * A slow heartbeat that re-offers the chooser to anyone who still has not picked. It covers the
     * player who logged out mid-choice and the player who finishes Tutorial Island mid-session with
     * one mechanism, so neither the login handler nor the tutorial module needs to know about the
     * other. It cancels itself the moment a tier is set.
     */
    val prompt = find("account_setup_prompt")
}
