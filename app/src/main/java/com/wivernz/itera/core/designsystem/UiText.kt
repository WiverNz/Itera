package com.wivernz.itera.core.designsystem
import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
sealed interface UiText {
    data class Raw(val value: String) : UiText
    data class Res(
        @param:StringRes val id: Int,
        val args: List<Any> =
            emptyList()
    ) : UiText
    data class Plural(
        @param:PluralsRes val id: Int,
        val count: Int,
        val args: List<Any> = emptyList()
    ) : UiText

    @Composable fun asString(): String = when (this) {
        is Raw -> value
        is Res -> stringResource(id, *args.toTypedArray())
        is Plural -> pluralStringResource(id, count, *args.toTypedArray())
    }
    fun asString(context: Context): String = when (this) {
        is Raw -> value
        is Res -> context.getString(id, *args.toTypedArray())
        is Plural ->
            context.resources.getQuantityString(
                id,
                count,
                *args.toTypedArray()
            )
    }
}
