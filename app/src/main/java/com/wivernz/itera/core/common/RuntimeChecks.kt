package com.wivernz.itera.core.common

/** Debug builds fail fast on programming errors; release builds degrade to a DomainError (docs/architecture/05). */
data class RuntimeChecks(val failFast: Boolean)
