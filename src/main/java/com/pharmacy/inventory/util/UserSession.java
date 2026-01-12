package com.pharmacy.inventory.util;

import com.pharmacy.inventory.model.User;

/**
 * Global session manager to track the logged-in staff member.
 */
public class UserSession {
    // Stores the user object globally during the app's lifecycle
    private static User currentUser;

    // Call this upon successful login
    public static void login(User user) {
        currentUser = user;
    }

    // Call this to clear data
    public static void logout() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    // Quick helper to check permissions
    public static String getUserRole() {
        return (currentUser != null) ? currentUser.getRole().toLowerCase() : "guest";
    }

    // Helper to check if someone is logged in
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}