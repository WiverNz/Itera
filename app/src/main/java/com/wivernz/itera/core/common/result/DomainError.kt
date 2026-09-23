package com.wivernz.itera.core.common.result
import com.wivernz.itera.domain.model.ActivityState
sealed interface DomainError {
    data class IllegalTransition(val from: ActivityState, val event: String) : DomainError
    data object ActivityNotFound : DomainError
    data object ReviewNotFound : DomainError
    data object CatalogUnavailable : DomainError
    data class ContentMissing(val techniqueId: String) : DomainError
    data class ExportFailed(val reason: String) : DomainError
}
class DomainException(val error: DomainError) : Exception(error.javaClass.simpleName)
