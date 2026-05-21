package com.example.aling_jar.data.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.aling_jar.R;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles uploading batch photos to Cloudinary and returning the HTTPS image URL.
 * <p>
 * Single Responsibility: This class ONLY handles the upload lifecycle.
 * It does NOT know about Firestore, batches, or UI — it just uploads a file
 * and returns a URL string.
 * <p>
 * Uses Cloudinary's REST API directly via signed upload — no extra SDK required.
 * Credentials are read from string resources (injected from local.properties
 * via build.gradle.kts) following the same pattern as the Facebook config.
 */
public class BatchPhotoUploader {

    private static final String TAG = "BatchPhotoUploader";
    private static final String CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/%s/image/upload";
    private static final String BOUNDARY = "----AlingJarUpload" + System.currentTimeMillis();
    private static final String LINE_END = "\r\n";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ─── Callback ──────────────────────────────────────────────────

    /** Callback for upload results. */
    public interface OnUploadCompleteListener {
        /** Called with the Cloudinary HTTPS image URL on success. */
        void onSuccess(@NonNull String photoUrl);

        /** Called with the exception on failure. */
        void onFailure(@NonNull Exception e);
    }

    // ─── Public API ────────────────────────────────────────────────

    /**
     * Uploads a local image to Cloudinary and returns a permanent HTTPS URL.
     * <p>
     * Runs the upload on a background thread and delivers the result
     * on the main thread via the provided callback.
     *
     * @param context  Android context (for ContentResolver and string resources).
     * @param fileUri  Content URI of the image to upload.
     * @param userId   UID of the user performing the upload (used as folder name).
     * @param listener Callback for success/failure.
     */
    public void uploadPhoto(
            @NonNull Context context,
            @NonNull Uri fileUri,
            @Nullable String userId,
            @NonNull OnUploadCompleteListener listener
    ) {
        // Read Cloudinary config from string resources
        String cloudName = context.getString(R.string.cloudinary_cloud_name);
        String apiKey = context.getString(R.string.cloudinary_api_key);
        String apiSecret = context.getString(R.string.cloudinary_api_secret);

        String folder = "batch_photos/" + (userId != null ? userId : "unknown");

        Log.d(TAG, "Starting Cloudinary upload → cloud: " + cloudName + ", folder: " + folder);

        executor.execute(() -> {
            try {
                // 1. Read the image bytes from the content URI
                byte[] imageBytes = readBytesFromUri(context, fileUri);
                Log.d(TAG, "Image loaded: " + imageBytes.length + " bytes");

                // 2. Prepare signed upload parameters
                long timestamp = System.currentTimeMillis() / 1000L;
                String signature = generateSignature(folder, timestamp, apiSecret);

                // 3. Upload via multipart POST
                String uploadUrl = String.format(CLOUDINARY_UPLOAD_URL, cloudName);
                String responseUrl = executeMultipartUpload(
                        uploadUrl, imageBytes, apiKey, signature, timestamp, folder
                );

                Log.d(TAG, "Upload succeeded! URL: " + responseUrl);

                // 4. Deliver result on main thread
                runOnMainThread(() -> listener.onSuccess(responseUrl));

            } catch (Exception e) {
                Log.e(TAG, "Cloudinary upload failed", e);
                runOnMainThread(() -> listener.onFailure(e));
            }
        });
    }

    // ─── Image Reading ────────────────────────────────────────────

    /**
     * Reads all bytes from a content:// URI via ContentResolver.
     */
    private byte[] readBytesFromUri(@NonNull Context context, @NonNull Uri uri) throws Exception {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new Exception("Could not open image from URI: " + uri);
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            return buffer.toByteArray();
        }
    }

    // ─── Signature Generation ─────────────────────────────────────

    /**
     * Generates a Cloudinary signed upload signature.
     * <p>
     * Formula: SHA-1 of "folder={folder}&timestamp={ts}{api_secret}"
     * Parameters must be sorted alphabetically before hashing.
     */
    private String generateSignature(String folder, long timestamp, String apiSecret) throws Exception {
        String toSign = "folder=" + folder + "&timestamp=" + timestamp + apiSecret;
        Log.d(TAG, "Signing: " + toSign.replace(apiSecret, "***"));

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(toSign.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // ─── HTTP Upload ──────────────────────────────────────────────

    /**
     * Executes a multipart/form-data POST to Cloudinary's upload endpoint.
     *
     * @return The {@code secure_url} from Cloudinary's JSON response.
     */
    private String executeMultipartUpload(
            String uploadUrl,
            byte[] imageBytes,
            String apiKey,
            String signature,
            long timestamp,
            String folder
    ) throws Exception {
        URL url = new URL(uploadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                // Write text fields
                writeFormField(out, "api_key", apiKey);
                writeFormField(out, "timestamp", String.valueOf(timestamp));
                writeFormField(out, "signature", signature);
                writeFormField(out, "folder", folder);

                // Write file field
                writeFileField(out, "file", "batch_photo.jpg", imageBytes);

                // End boundary
                out.writeBytes("--" + BOUNDARY + "--" + LINE_END);
                out.flush();
            }

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Cloudinary response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read success response
                String responseBody = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(responseBody);
                String secureUrl = json.getString("secure_url");
                Log.d(TAG, "Cloudinary secure_url: " + secureUrl);
                return secureUrl;
            } else {
                // Read error response
                String errorBody = readStream(conn.getErrorStream());
                Log.e(TAG, "Cloudinary error (" + responseCode + "): " + errorBody);
                throw new Exception("Upload failed (HTTP " + responseCode + "): " + errorBody);
            }
        } finally {
            conn.disconnect();
        }
    }

    // ─── Multipart Helpers ────────────────────────────────────────

    private void writeFormField(DataOutputStream out, String name, String value) throws Exception {
        out.writeBytes("--" + BOUNDARY + LINE_END);
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + LINE_END);
        out.writeBytes(LINE_END);
        out.writeBytes(value + LINE_END);
    }

    private void writeFileField(DataOutputStream out, String fieldName, String fileName, byte[] fileBytes)
            throws Exception {
        out.writeBytes("--" + BOUNDARY + LINE_END);
        out.writeBytes("Content-Disposition: form-data; name=\"" + fieldName
                + "\"; filename=\"" + fileName + "\"" + LINE_END);
        out.writeBytes("Content-Type: image/jpeg" + LINE_END);
        out.writeBytes(LINE_END);
        out.write(fileBytes);
        out.writeBytes(LINE_END);
    }

    private String readStream(InputStream stream) throws Exception {
        if (stream == null) return "";
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = stream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }

    // ─── Threading ────────────────────────────────────────────────

    private void runOnMainThread(Runnable action) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(action);
    }
}
