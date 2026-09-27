package com.wivernz.itera.di

import com.wivernz.itera.core.voice.AndroidVoiceRecognizer
import com.wivernz.itera.core.voice.VoiceRecognizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/** One recogniser per activity: it is created, used and destroyed on the main thread with the UI. */
@Module
@InstallIn(ActivityComponent::class)
abstract class VoiceModule {
    @Binds
    abstract fun recognizer(impl: AndroidVoiceRecognizer): VoiceRecognizer
}
