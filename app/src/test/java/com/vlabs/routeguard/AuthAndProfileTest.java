package com.vlabs.routeguard;

import com.vlabs.routeguard.models.User;
import com.vlabs.routeguard.utils.AuthenticationValidator;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 28, packageName = "com.vlabs.routeguard")
public class AuthAndProfileTest {

    @Test
    public void testAuthenticationValidator_EmailValidation() {
        assertTrue("Valid email should pass", AuthenticationValidator.isValidEmail("kuppaabhi2@gmail.com"));
        assertTrue("Valid email with dots should pass", AuthenticationValidator.isValidEmail("john.doe@example.org"));
        assertFalse("Missing @ should fail", AuthenticationValidator.isValidEmail("invalidemail.com"));
        assertFalse("Empty email should fail", AuthenticationValidator.isValidEmail(""));
        assertFalse("Null email should fail", AuthenticationValidator.isValidEmail(null));
    }

    @Test
    public void testAuthenticationValidator_PasswordValidation() {
        assertTrue("Password >= 6 characters should pass", AuthenticationValidator.isValidPassword("qwertyuiop"));
        assertTrue("Exact 6 characters should pass", AuthenticationValidator.isValidPassword("123456"));
        assertFalse("Password < 6 characters should fail", AuthenticationValidator.isValidPassword("12345"));
        assertFalse("Empty password should fail", AuthenticationValidator.isValidPassword(""));
        assertFalse("Null password should fail", AuthenticationValidator.isValidPassword(null));
    }

    @Test
    public void testUserDefaultProfileCreation() {
        String testUid = "firebase_auth_user_999";
        String testEmail = "kuppaabhi2@gmail.com";
        String displayName = "kuppaabhi2";

        User user = new User(
                testUid,
                displayName,
                testEmail,
                null,
                0,
                0,
                50,
                50,
                false,
                false
        );

        assertEquals(testUid, user.getUserId());
        assertEquals(displayName, user.getDisplayName());
        assertEquals(testEmail, user.getEmail());
        assertNull(user.getPhotoUrl());
        assertEquals(50, user.getTrustScore());
        assertEquals(50, user.getReputationScore());
        assertFalse(user.isModerator());
        assertFalse(user.isBlocked());
    }

    @Test
    public void testLocalUserJsonSerialization() {
        com.vlabs.routeguard.managers.LocalAuthManager.LocalUser localUser =
                new com.vlabs.routeguard.managers.LocalAuthManager.LocalUser(
                        "usr_test_123",
                        "test@example.com",
                        "secret123",
                        "Tester",
                        123456789L
                );

        org.json.JSONObject json = localUser.toJson();
        assertEquals("usr_test_123", json.optString("userId"));
        assertEquals("test@example.com", json.optString("email"));
        assertEquals("secret123", json.optString("password"));
        assertEquals("Tester", json.optString("displayName"));

        com.vlabs.routeguard.managers.LocalAuthManager.LocalUser restored =
                com.vlabs.routeguard.managers.LocalAuthManager.LocalUser.fromJson(json);
        assertNotNull(restored);
        assertEquals(localUser.userId, restored.userId);
        assertEquals(localUser.email, restored.email);
        assertEquals(localUser.password, restored.password);
        assertEquals(localUser.displayName, restored.displayName);
        assertEquals(localUser.createdAt, restored.createdAt);
    }

    @Test
    public void testNotificationPreferencesAndDndDefaults() {
        android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        com.vlabs.routeguard.utils.AppPreferences prefs = new com.vlabs.routeguard.utils.AppPreferences(context);

        // Verify default states
        assertTrue(prefs.isNotificationsEnabled());
        assertTrue(prefs.isHazardAlertsEnabled());
        assertTrue(prefs.isCommunityAlertsEnabled());
        assertTrue(prefs.isSoundEnabled());
        assertTrue(prefs.isVibrationEnabled());
        assertFalse(prefs.isDndEnabled());

        // Test mutating and reading preferences
        prefs.setDndEnabled(true);
        assertTrue(prefs.isDndEnabled());
        prefs.setDndEnabled(false);
        assertFalse(prefs.isDndEnabled());

        prefs.setHazardAlertsEnabled(false);
        assertFalse(prefs.isHazardAlertsEnabled());
        prefs.setHazardAlertsEnabled(true);
        assertTrue(prefs.isHazardAlertsEnabled());
    }

    @Test
    public void testAccountSinceDateFormatting() {
        long testTimestamp = 1729881600000L; // Oct 25, 2024
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US);
        String formatted = sdf.format(new java.util.Date(testTimestamp));
        assertNotNull(formatted);
        assertTrue(formatted.contains("2024"));
    }
}

