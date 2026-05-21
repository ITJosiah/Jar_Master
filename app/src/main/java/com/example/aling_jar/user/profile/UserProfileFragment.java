package com.example.aling_jar.user.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.aling_jar.R;
import com.example.aling_jar.auth.LoginActivity;
import com.example.aling_jar.data.model.User;
import com.example.aling_jar.data.repository.BatchPhotoUploader;
import com.example.aling_jar.user.UserActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileFragment extends Fragment {

    private static final String TAG = "UserProfileFragment";

    private ImageView ivProfileImage;
    private TextView tvUserName, tvUserEmail;
    private MaterialButton btnLogout;
    private View fabEditImage;

    private View rowMyOrders, rowSavedItems, rowAccountInfo, rowChangePassword, rowAbout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BatchPhotoUploader photoUploader;

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        photoUploader = new BatchPhotoUploader();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadProfileImage(uri);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRow(rowMyOrders, R.drawable.ic_orders, R.string.profile_my_orders, v -> {
            if (getActivity() instanceof UserActivity) {
                ((UserActivity) getActivity()).navigateToOrders();
            }
        });
        setupRow(rowSavedItems, R.drawable.saved_ic, R.string.profile_saved_items, v -> {
            if (getActivity() instanceof UserActivity) {
                ((UserActivity) getActivity()).navigateToSaved();
            }
        });
        setupRow(rowAccountInfo, R.drawable.ic_person, R.string.profile_account_info, v -> {
            Intent intent = new Intent(getActivity(), EditAccountActivity.class);
            startActivity(intent);
        });
        setupRow(rowChangePassword, R.drawable.ic_lock, R.string.profile_change_password, v -> {
            Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });
        setupRow(rowAbout, R.drawable.ic_overview, R.string.profile_about, v -> {
            Intent intent = new Intent(getActivity(), AboutAlingActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> handleLogout());
        fabEditImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        loadUserData();
    }

    private void initViews(View view) {
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        btnLogout = view.findViewById(R.id.btnLogout);
        fabEditImage = view.findViewById(R.id.fabEditImage);

        rowMyOrders = view.findViewById(R.id.rowMyOrders);
        rowSavedItems = view.findViewById(R.id.rowSavedItems);
        rowAccountInfo = view.findViewById(R.id.rowAccountInfo);
        rowChangePassword = view.findViewById(R.id.rowChangePassword);
        rowAbout = view.findViewById(R.id.rowAbout);
    }

    private void setupRow(View rowView, int iconRes, int titleRes, View.OnClickListener listener) {
        ImageView ivIcon = rowView.findViewById(R.id.ivRowIcon);
        TextView tvTitle = rowView.findViewById(R.id.tvRowTitle);

        ivIcon.setImageResource(iconRes);
        tvTitle.setText(titleRes);
        rowView.setOnClickListener(listener);
    }

    private void loadUserData() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            updateUI(user);
                        }
                    }
                });
    }

    private void updateUI(User user) {
        if (!isAdded()) return;

        tvUserName.setText(user.getFullName());
        tvUserEmail.setText(user.getEmail());

        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(ivProfileImage);
        }
    }

    private void uploadProfileImage(Uri uri) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        Toast.makeText(getContext(), "Uploading profile image...", Toast.LENGTH_SHORT).show();

        photoUploader.uploadPhoto(requireContext(), uri, uid, new BatchPhotoUploader.OnUploadCompleteListener() {
            @Override
            public void onSuccess(@NonNull String photoUrl) {
                if (!isAdded()) return;
                
                db.collection("users").document(uid)
                        .update("photoUrl", photoUrl)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Profile image updated!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update profile URL", e);
                            Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleLogout() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
