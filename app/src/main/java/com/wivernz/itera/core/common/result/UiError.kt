package com.wivernz.itera.core.common.result

import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.UiText

fun Throwable.toUiText(): UiText = UiText.Res(
    when ((this as? DomainException)?.error) {
        is DomainError.IllegalTransition -> R.string.error_transition
        DomainError.ActivityNotFound -> R.string.error_activity_missing
        DomainError.ReviewNotFound -> R.string.error_review_missing
        DomainError.CatalogUnavailable,
        is DomainError.ContentMissing ->
            R.string.error_content_missing
        is DomainError.ExportFailed -> R.string.error_export
        null -> R.string.error_generic
    }
)
