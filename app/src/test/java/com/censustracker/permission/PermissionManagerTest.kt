package com.censustracker.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionManagerTest {

    @Test
    fun testRequiredPermissionsCompleteness() {
        val permissions = PermissionManager.REQUIRED_PERMISSIONS
        assertEquals(5, permissions.size)

        val permissionNames = permissions.map { it.permission }
        assertTrue(permissionNames.contains(android.Manifest.permission.SEND_SMS))
        assertTrue(permissionNames.contains(android.Manifest.permission.RECEIVE_SMS))
        assertTrue(permissionNames.contains(android.Manifest.permission.READ_SMS))
        assertTrue(permissionNames.contains(android.Manifest.permission.WRITE_CONTACTS))
        assertTrue(permissionNames.contains(android.Manifest.permission.READ_CONTACTS))
    }

    @Test
    fun testPermissionExplanationsPresent() {
        for (item in PermissionManager.REQUIRED_PERMISSIONS) {
            assertTrue("Title should not be blank for ${item.permission}", item.title.isNotBlank())
            assertTrue("Description should explain rationale for ${item.permission}", item.description.isNotBlank())
        }
    }
}
