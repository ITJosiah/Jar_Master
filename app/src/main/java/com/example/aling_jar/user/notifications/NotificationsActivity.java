package com.example.aling_jar.user.notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aling_jar.R;
import com.example.aling_jar.data.model.Notification;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationsActivity";

    private RecyclerView rvNotifications;
    private LinearLayout llEmptyState;
    private NotificationAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbarNotifications);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Inflate the menu with the Clear All action
        toolbar.inflateMenu(R.menu.menu_notifications);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_all) {
                confirmClearAll();
                return true;
            }
            return false;
        });

        rvNotifications = findViewById(R.id.rvNotifications);
        llEmptyState = findViewById(R.id.llEmptyState);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter();
        rvNotifications.setAdapter(adapter);

        listenForNotifications();
    }

    private void listenForNotifications() {
        String uid = mAuth.getUid();
        if (uid == null) {
            showEmptyState();
            return;
        }

        listenerRegistration = db.collection("users")
                .document(uid)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading notifications", error);
                        return;
                    }

                    List<Notification> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String id      = doc.getId();
                            String title   = doc.getString("title");
                            String message = doc.getString("message");
                            String type    = doc.getString("type");
                            Long ts        = doc.getLong("timestamp");
                            Boolean read   = doc.getBoolean("isRead");

                            Notification n = new Notification(
                                    id,
                                    title   != null ? title   : "",
                                    message != null ? message : "",
                                    ts      != null ? ts      : 0L,
                                    read    != null && read,
                                    type    != null ? type    : "SYSTEM"
                            );
                            list.add(n);
                        }
                    }

                    if (list.isEmpty()) {
                        showEmptyState();
                    } else {
                        rvNotifications.setVisibility(View.VISIBLE);
                        llEmptyState.setVisibility(View.GONE);
                        adapter.setNotifications(list);
                    }
                });
    }

    private void showEmptyState() {
        rvNotifications.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.VISIBLE);
    }

    /** Marks every unread notification as read when the user leaves the screen. */
    private void markAllRead(String uid) {
        db.collection("users")
                .document(uid)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snapshots -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        batch.update(doc.getReference(), "isRead", true);
                    }
                    batch.commit().addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to mark all as read", e);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching unread notifications", e);
                });
    }

    private void confirmClearAll() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear Notifications")
                .setMessage("Are you sure you want to delete all notifications? This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> clearAllNotifications(uid))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllNotifications(String uid) {
        db.collection("users")
                .document(uid)
                .collection("notifications")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        Toast.makeText(this, "No notifications to clear", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error clearing notifications", e);
                        Toast.makeText(this, "Failed to clear notifications", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching notifications for deletion", e);
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        String uid = mAuth.getUid();
        if (uid != null) {
            markAllRead(uid);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) listenerRegistration.remove();
    }
}
