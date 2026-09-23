package com.wivernz.itera.analytics
class NoOpAnalytics : Analytics {
    override fun track(event: Event) = Unit
}
