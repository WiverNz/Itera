package com.wivernz.itera.feature.voice

import com.wivernz.itera.core.voice.VoiceAvailability
import com.wivernz.itera.core.voice.VoiceError
import com.wivernz.itera.core.voice.VoiceRecognitionListener
import com.wivernz.itera.core.voice.VoiceRecognizer
import com.wivernz.itera.domain.voice.VoiceCommand
import com.wivernz.itera.domain.voice.VoiceCommandKind
import com.wivernz.itera.domain.voice.VoiceLanguage
import java.io.File

/** Deterministic recogniser: tests drive partial/final/error callbacks, including late ones. */
class FakeRecognizer(var available: Boolean = true) : VoiceRecognizer {
    val languages = mutableListOf<String>()
    val listeners = mutableListOf<VoiceRecognitionListener>()
    var stops = 0
    var cancels = 0
    var releases = 0

    val listener: VoiceRecognitionListener get() = listeners.last()

    override fun availability() =
        if (available) VoiceAvailability.AVAILABLE else VoiceAvailability.UNAVAILABLE

    override fun start(languageTag: String, listener: VoiceRecognitionListener) {
        languages += languageTag
        listeners += listener
    }

    override fun stop() {
        stops++
    }

    override fun cancel() {
        cancels++
    }

    override fun release() {
        releases++
    }

    fun partial(text: String) = listener.onPartial(text)

    fun final(vararg alternatives: String) = listener.onFinal(alternatives.toList())

    fun error(error: VoiceError) = listener.onError(error)
}

class FakeGate(
    var granted: Boolean = true,
    var permanent: Boolean = false,
    var grantOnRequest: Boolean = true
) : VoicePermissionGate {
    var requests = 0

    override fun granted() = granted

    override fun request(onResult: (Boolean) -> Unit) {
        requests++
        if (grantOnRequest) granted = true
        onResult(granted)
    }

    override fun permanentlyDenied() = !granted && permanent
}

fun voiceController(
    recognizer: FakeRecognizer = FakeRecognizer(),
    gate: FakeGate = FakeGate(),
    language: VoiceLanguage = VoiceLanguage.EN
) = VoiceController(recognizer, gate, language)

/** Records executions; [plan] decides what each command becomes. */
class FakeHost(
    override val voiceCommands: Set<VoiceCommandKind> = VoiceCommandKind.entries.toSet(),
    var plan: (VoiceCommand) -> VoicePlan = { VoicePlan.Reject(VoiceRejection.NotHere) },
    var outcome: suspend (VoiceAction) -> VoiceOutcome = {
        VoiceOutcome.Done(VoiceFeedback.FocusPaused)
    }
) : VoiceCommandHost {
    val planned = mutableListOf<VoiceCommand>()
    val executed = mutableListOf<VoiceAction>()

    override fun planVoice(command: VoiceCommand): VoicePlan {
        planned += command
        return plan(command)
    }

    override suspend fun executeVoice(action: VoiceAction): VoiceOutcome {
        executed += action
        return outcome(action)
    }
}

/** Production sources, relative to the app module (the unit-test working directory). */
val mainSources = File("src/main/java/com/wivernz/itera")

fun sourcesUnder(vararg dirs: String): List<File> = dirs.flatMap { dir ->
    File(mainSources, dir).walkTopDown().filter { it.extension == "kt" }.toList()
}
