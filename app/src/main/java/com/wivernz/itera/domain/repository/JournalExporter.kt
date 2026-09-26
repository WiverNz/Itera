package com.wivernz.itera.domain.repository

import com.wivernz.itera.analytics.ExportRange
import java.io.File
import java.util.Locale

interface JournalExporter {
    suspend fun export(range: ExportRange, locale: Locale): File
    suspend fun cleanup(file: File)
}
