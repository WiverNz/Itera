package com.wivernz.itera.di

import com.wivernz.itera.core.voice.AndroidSpeechPlatform
import com.wivernz.itera.core.voice.AndroidVoiceRecognizer
import com.wivernz.itera.core.voice.OfflineModels
import com.wivernz.itera.core.voice.OfflineSpeechEngine
import com.wivernz.itera.core.voice.SpeechPlatform
import com.wivernz.itera.core.voice.VoiceConsentStore
import com.wivernz.itera.core.voice.VoiceModelLoader
import com.wivernz.itera.core.voice.VoiceModelStore
import com.wivernz.itera.core.voice.VoiceRecognizer
import com.wivernz.itera.core.voice.VoskModelLoader
import com.wivernz.itera.core.voice.VoskSpeechEngine
import com.wivernz.itera.data.preferences.PreferencesVoiceConsentStore
import com.wivernz.itera.data.voicemodel.AndroidVoiceModelStore
import com.wivernz.itera.data.voicemodel.ModelPacks
import com.wivernz.itera.data.voicemodel.PlayModelPacks
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.components.SingletonComponent

/** One recogniser per activity: it is created, used and destroyed on the main thread with the UI. */
@Module
@InstallIn(ActivityComponent::class)
abstract class VoiceModule {
    @Binds
    abstract fun recognizer(impl: AndroidVoiceRecognizer): VoiceRecognizer

    @Binds
    abstract fun platform(impl: AndroidSpeechPlatform): SpeechPlatform
}

/** Recogniser consent and choice outlive activities and are shared with Settings. */
@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceConsentModule {
    @Binds
    abstract fun consent(impl: PreferencesVoiceConsentStore): VoiceConsentStore

    // milestone 013: the built-in offline recogniser and its model store are app-wide
    @Binds
    abstract fun engine(impl: VoskSpeechEngine): OfflineSpeechEngine

    @Binds
    abstract fun modelStore(impl: AndroidVoiceModelStore): VoiceModelStore

    @Binds
    abstract fun offlineModels(impl: AndroidVoiceModelStore): OfflineModels

    @Binds
    abstract fun packs(impl: PlayModelPacks): ModelPacks

    @Binds
    abstract fun loader(impl: VoskModelLoader): VoiceModelLoader
}
