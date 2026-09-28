package com.wivernz.itera

import android.app.Application
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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

    /**
     * Every permission in the merged manifest, including libraries': any addition must be reviewed here. WorkManager
     * brings ACCESS_NETWORK_STATE and WAKE_LOCK; Play Asset Delivery (milestone 013) brings the data-sync foreground
     * service it uses to extract model packs. None grants network access.
     */
    @Test
    fun mergedPermissionsAreExactlyTheReviewedSet() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val permissions = app.packageManager.getPackageInfo(
            app.packageName,
            PackageManager.GET_PERMISSIONS
        ).requestedPermissions.orEmpty().toSet() -
            "${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        assertEquals(
            setOf(
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.RECEIVE_BOOT_COMPLETED",
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.WAKE_LOCK",
                "android.permission.FOREGROUND_SERVICE_DATA_SYNC"
            ),
            permissions
        )
    }
}
