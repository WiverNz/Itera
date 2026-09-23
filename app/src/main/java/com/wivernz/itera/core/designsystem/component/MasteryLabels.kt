package com.wivernz.itera.core.designsystem.component
import com.wivernz.itera.R
import com.wivernz.itera.domain.model.MasteryLevel
val MasteryLevel.title: Int get() = when (this) {
    MasteryLevel.NONE -> R.string.not_started
    MasteryLevel.MET -> R.string.level_met
    MasteryLevel.PRACTICED -> R.string.level_practiced
    MasteryLevel.APPLIED -> R.string.level_applied
    MasteryLevel.INTEGRATED -> R.string.level_integrated
}
