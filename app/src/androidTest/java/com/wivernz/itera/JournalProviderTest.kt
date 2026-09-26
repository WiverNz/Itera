package com.wivernz.itera

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wivernz.itera.feature.you.journalShareIntent
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JournalProviderTest {
    @Test fun realProviderGrantsOnlyJournalReadAccess() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "export/provider-test.md")
        file.parentFile!!.mkdirs()
        file.writeText("# Synthetic test journal\n")
        try {
            val intent = journalShareIntent(context, file)
            val uri = intent.clipData!!.getItemAt(0).uri
            assertEquals("text/markdown", intent.type)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertEquals(
                file.readText(),
                context.contentResolver.openInputStream(uri)!!.bufferedReader().use {
                    it.readText()
                }
            )
            val provider = context.packageManager.resolveContentProvider(uri.authority!!, 0)!!
            assertFalse(provider.exported)
            assertTrue(provider.grantUriPermissions)
        } finally {
            file.delete()
        }
    }
}
