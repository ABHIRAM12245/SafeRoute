package com.vlabs.routeguard.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages file-based user accounts and sessions directly on device storage.
 * Completely eliminates the need for an external user database for login and registration.
 */
public class LocalAuthManager {

    private static final String TAG = "LocalAuthManager";
    private static final String ACCOUNTS_FILE_NAME = "local_accounts.json";
    private static final String PREF_NAME = "routeguard_local_session";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "active_user_id";
    private static final String KEY_EMAIL = "active_email";
    private static final String KEY_DISPLAY_NAME = "active_display_name";

    private static LocalAuthManager instance;

    public static class LocalUser {
        public final String userId;
        public final String email;
        public final String password;
        public final String displayName;
        public final long createdAt;

        public LocalUser(String userId, String email, String password, String displayName, long createdAt) {
            this.userId = userId;
            this.email = email;
            this.password = password;
            this.displayName = displayName;
            this.createdAt = createdAt;
        }

        public JSONObject toJson() {
            try {
                JSONObject json = new JSONObject();
                json.put("userId", userId);
                json.put("email", email);
                json.put("password", password);
                json.put("displayName", displayName);
                json.put("createdAt", createdAt);
                return json;
            } catch (Exception e) {
                return new JSONObject();
            }
        }

        public static LocalUser fromJson(JSONObject json) {
            if (json == null) return null;
            return new LocalUser(
                    json.optString("userId"),
                    json.optString("email"),
                    json.optString("password"),
                    json.optString("displayName", "User"),
                    json.optLong("createdAt", System.currentTimeMillis())
            );
        }
    }

    private LocalAuthManager() {
    }

    public static synchronized LocalAuthManager getInstance() {
        if (instance == null) {
            instance = new LocalAuthManager();
        }
        return instance;
    }

    private Context getContext() {
        try {
            return FirebaseApp.getInstance().getApplicationContext();
        } catch (Exception e) {
            Log.w(TAG, "Failed to get Context from FirebaseApp: " + e.getMessage());
            return null;
        }
    }

    /**
     * Authenticates a user against the local accounts file.
     *
     * @param email    The user's email address.
     * @param password The user's password.
     * @return LocalUser if credentials match, null otherwise.
     */
    public synchronized LocalUser authenticate(String email, String password) {
        if (email == null || password == null) return null;

        String targetEmail = email.trim().toLowerCase();
        List<LocalUser> users = loadUsersFromFile();

        for (LocalUser user : users) {
            if (user.email != null && user.email.trim().toLowerCase().equals(targetEmail)) {
                if (password.equals(user.password)) {
                    saveActiveSession(user);
                    attemptBackgroundFirebaseAuth(user.email, password);
                    return user;
                }
            }
        }

        return null;
    }

    /**
     * Registers and saves a new user account into the local file.
     *
     * @param uid         Optional UID; if null, a new UUID is generated.
     * @param email       The user's email.
     * @param password    The user's password.
     * @param displayName The user's display name.
     * @return The created or updated LocalUser.
     */
    public synchronized LocalUser saveUser(String uid, String email, String password, String displayName) {
        if (email == null || email.trim().isEmpty()) return null;

        String cleanEmail = email.trim().toLowerCase();
        String userId = (uid != null && !uid.trim().isEmpty()) ? uid : "usr_" + UUID.randomUUID().toString().substring(0, 12);
        String name = (displayName != null && !displayName.trim().isEmpty()) ? displayName : cleanEmail.split("@")[0];

        List<LocalUser> users = loadUsersFromFile();
        LocalUser existingUser = null;
        int existingIndex = -1;

        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).email != null && users.get(i).email.trim().toLowerCase().equals(cleanEmail)) {
                existingUser = users.get(i);
                existingIndex = i;
                break;
            }
        }

        LocalUser userToSave;
        if (existingUser != null) {
            userToSave = new LocalUser(existingUser.userId, cleanEmail, password, name, existingUser.createdAt);
            users.set(existingIndex, userToSave);
        } else {
            userToSave = new LocalUser(userId, cleanEmail, password, name, System.currentTimeMillis());
            users.add(userToSave);
        }

        saveUsersToFile(users);
        saveActiveSession(userToSave);
        attemptBackgroundFirebaseAuth(cleanEmail, password);
        return userToSave;
    }

    /**
     * Returns true if there is an active logged-in session.
     */
    public boolean isLoggedIn() {
        Context ctx = getContext();
        if (ctx == null) return false;
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    /**
     * Returns the currently active logged-in user, or null if logged out.
     */
    public LocalUser getActiveUser() {
        Context ctx = getContext();
        if (ctx == null) return null;
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_LOGGED_IN, false)) return null;

        return new LocalUser(
                prefs.getString(KEY_USER_ID, ""),
                prefs.getString(KEY_EMAIL, ""),
                "",
                prefs.getString(KEY_DISPLAY_NAME, "User"),
                0
        );
    }

    /**
     * Returns the active user's ID, or empty string if not logged in.
     */
    public String getActiveUserId() {
        LocalUser user = getActiveUser();
        return user != null && user.userId != null ? user.userId : "";
    }

    /**
     * Logs out the active user and clears the session.
     */
    public void logout() {
        Context ctx = getContext();
        if (ctx == null) return;
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    private void saveActiveSession(LocalUser user) {
        Context ctx = getContext();
        if (ctx == null || user == null) return;

        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_USER_ID, user.userId)
                .putString(KEY_EMAIL, user.email)
                .putString(KEY_DISPLAY_NAME, user.displayName)
                .apply();
    }

    private List<LocalUser> loadUsersFromFile() {
        List<LocalUser> list = new ArrayList<>();
        Context ctx = getContext();
        if (ctx == null) return list;

        File file = new File(ctx.getFilesDir(), ACCOUNTS_FILE_NAME);
        if (!file.exists()) {
            // Seed initial account so existing user can log in immediately
            LocalUser defaultUser = new LocalUser(
                    "usr_kuppaabhi2",
                    "kuppaabhi2@gmail.com",
                    "qwertyuiop",
                    "kuppaabhi2",
                    System.currentTimeMillis()
            );
            list.add(defaultUser);
            saveUsersToFile(list);
            return list;
        }

        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                LocalUser u = LocalUser.fromJson(array.getJSONObject(i));
                if (u != null) {
                    list.add(u);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading local accounts file: " + e.getMessage());
        }

        return list;
    }

    private void saveUsersToFile(List<LocalUser> users) {
        Context ctx = getContext();
        if (ctx == null) return;

        File file = new File(ctx.getFilesDir(), ACCOUNTS_FILE_NAME);
        try {
            JSONArray array = new JSONArray();
            for (LocalUser u : users) {
                array.put(u.toJson());
            }
            try (FileOutputStream fos = new FileOutputStream(file, false)) {
                fos.write(array.toString(2).getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving local accounts file: " + e.getMessage());
        }
    }

    private void attemptBackgroundFirebaseAuth(String email, String password) {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) return;

            auth.signInWithEmailAndPassword(email, password)
                    .addOnFailureListener(e -> {
                        // If account does not exist in Firebase Auth, sign in anonymously so backend features work
                        try {
                            auth.signInAnonymously()
                                    .addOnFailureListener(err -> Log.d(TAG, "Firebase offline: " + err.getMessage()));
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception e) {
            Log.d(TAG, "Firebase background auth check skipped: " + e.getMessage());
        }
    }
}
