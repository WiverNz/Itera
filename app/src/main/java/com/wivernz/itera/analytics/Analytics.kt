package com.wivernz.itera.analytics
interface Analytics {
    fun track(event: Event)

    /**
     * Appends [event] in the caller's coroutine, so a write inside a database transaction commits or rolls
     * back with it (completion effect 7). Never throws for an analytics failure.
     */
    suspend fun append(event: Event) = track(event)
}
