package com.wivernz.itera

import android.app.Application
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** PackageManager reads the AGP-merged manifest, including library permissions. */
@RunWith(RobolectricTestRunner::class)
class OfflineTest {
    @Test
    fun mergedManifestHasNoInternetPermission() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val permissions = app.packageManager.getPackageInfo(
            app.packageName,
            PackageManager.GET_PERMISSIONS
        ).requestedPermissions.orEmpty()
        assertFalse(permissions.contains("android.permission.INTERNET"))
    }
}
