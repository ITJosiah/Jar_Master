package com.example.aling_jar.utils;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for writing notification documents to Firestore.
 *
 * Notifications are stored under: users/{userId}/notifications/{notificationId}
 * The user-side NotificationsActivity reads from this sub-collection.
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    /**
     * Writes a notification to a user's Firestore sub-collection.
     *
     * @param userId  The UID of the user to notify.
     * @param title   Short headline for the notification.
     * @param message Detailed message body.
     * @param type    Notification category e.g. "ORDER_UPDATE", "PROMO".
     */
    public static void sendNotification(String userId, String title, String message, String type) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "sendNotification: userId is null/empty, skipping.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", type);
        notification.put("timestamp", Timestamp.now().getSeconds() * 1000L); // ms epoch
        notification.put("isRead", false);

        db.collection("users")
                .document(userId)
                .collection("notifications")
                .add(notification)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Notification sent to user " + userId + ": " + title);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send notification to user " + userId, e);
                });
    }

    // ── Convenience helpers for every order status ──────────────────

    public static void notifyOrderConfirmed(String userId, String orderId) {
        sendNotification(
                userId,
                "Order Confirmed! ✅",
                "Your order #" + orderId + " has been confirmed and is being prepared.",
                "ORDER_UPDATE"
        );
    }

    public static void notifyOrderDeclined(String userId, String orderId) {
        sendNotification(
                userId,
                "Order Declined ❌",
                "Unfortunately, your order #" + orderId + " has been declined. Please contact us for more details.",
                "ORDER_UPDATE"
        );
    }

    public static void notifyOrderOutForDelivery(String userId, String orderId) {
        sendNotification(
                userId,
                "Out for Delivery 🛵",
                "Great news! Your order #" + orderId + " is on its way to you. Get ready!",
                "ORDER_UPDATE"
        );
    }
}
