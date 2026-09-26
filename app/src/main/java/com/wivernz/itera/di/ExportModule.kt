package com.wivernz.itera.di
import com.wivernz.itera.data.export.ExportJournalUseCase
import com.wivernz.itera.domain.repository.JournalExporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {
    @Binds abstract fun exporter(implementation: ExportJournalUseCase): JournalExporter
}
