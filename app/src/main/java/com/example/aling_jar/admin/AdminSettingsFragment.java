package com.example.aling_jar.admin;

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
import com.example.aling_jar.data.repository.BatchPhotoUploader;
import com.example.aling_jar.user.profile.ChangePasswordActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminSettingsFragment extends Fragment {

    private static final String TAG = "AdminProfileFragment";

    private ImageView ivProfileImage;
    private View fabEditImage;
    private TextView tvUserName, tvUserEmail;
    private MaterialButton btnLogout;
    private View rowChangePassword;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        setupRow(rowChangePassword, R.drawable.ic_lock, R.string.profile_change_password, v -> {
            Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                getActivity().finish();
            }
        });

        fabEditImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        loadUserProfile();
    }

    private void initViews(View view) {
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        fabEditImage = view.findViewById(R.id.fabEditImage);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        btnLogout = view.findViewById(R.id.btnLogout);

        rowChangePassword = view.findViewById(R.id.rowChangePassword);
    }

    private void setupRow(View rowView, int iconRes, int titleRes, View.OnClickListener listener) {
        ImageView ivIcon = rowView.findViewById(R.id.ivRowIcon);
        TextView tvTitle = rowView.findViewById(R.id.tvRowTitle);

        ivIcon.setImageResource(iconRes);
        tvTitle.setText(titleRes);
        rowView.setOnClickListener(listener);
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "No email");

            db.collection("users")
                    .document(user.getUid())
                    .addSnapshotListener((doc, e) -> {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        if (doc != null && doc.exists()) {
                            if (doc.getString("fullName") != null) {
                                tvUserName.setText(doc.getString("fullName"));
                            } else {
                                tvUserName.setText(user.getDisplayName() != null ?
                                        user.getDisplayName() : "Admin");
                            }

                            String photoUrl = doc.getString("photoUrl");
                            if (photoUrl != null && !photoUrl.isEmpty() && isAdded()) {
                                Glide.with(this)
                                        .load(photoUrl)
                                        .placeholder(R.drawable.ic_profile_placeholder)
                                        .circleCrop()
                                        .into(ivProfileImage);
                            }
                        }
                    });
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
}
