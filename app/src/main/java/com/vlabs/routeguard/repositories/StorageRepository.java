package com.vlabs.routeguard.repositories;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.vlabs.routeguard.models.PhotoAttachment;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Foundation architecture for Cloudinary Storage operations.
 */
public class StorageRepository {

    private static final String CLOUD_NAME = "ql4tniy5";
    private static final String UPLOAD_PRESET = "routeguard_upload";
    private static final String CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

    private final OkHttpClient client;
    private final Handler mainHandler;

    public interface UploadCallback {
        void onSuccess(String downloadUrl, String publicId);
        void onFailure(Exception e);
    }

    public StorageRepository() {
        this.client = new OkHttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Legacy synchronous stub method. Maintained so UploadManager does not break.
     * @param attachment The PhotoAttachment to upload.
     * @return An empty string.
     */
    public String upload(PhotoAttachment attachment) {
        return "";
    }

    /**
     * Uploads a photo attachment to Cloudinary asynchronously.
     *
     * @param attachment The PhotoAttachment to upload.
     * @param callback The callback to return success (secure URL) or failure.
     */
    public void upload(PhotoAttachment attachment, UploadCallback callback) {
        if (attachment == null || attachment.getUri() == null) {
            callback.onFailure(new IllegalArgumentException("Invalid attachment or URI"));
            return;
        }

        File imageFile = new File(attachment.getUri().getPath());
        if (!imageFile.exists()) {
            callback.onFailure(new Exception("Compressed file not found"));
            return;
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart("file", imageFile.getName(),
                        RequestBody.create(MediaType.parse("image/jpeg"), imageFile))
                .build();

        Request request = new Request.Builder()
                .url(CLOUDINARY_URL)
                .post(requestBody)
                .build();

        android.util.Log.d("StorageRepository", "1. Upload URL: " + CLOUDINARY_URL);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("StorageRepository", "6/7. onFailure() callback - IOException: ", e);
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                android.util.Log.d("StorageRepository", "2. HTTP response code: " + response.code());
                String responseBody = response.body() != null ? response.body().string() : "null";
                android.util.Log.d("StorageRepository", "3. Response body from Cloudinary: " + responseBody);

                if (!response.isSuccessful()) {
                    android.util.Log.e("StorageRepository", "Error JSON completely: " + responseBody);
                    Exception e = new Exception("Upload failed: " + response.code());
                    android.util.Log.e("StorageRepository", "6. onFailure() callback with HTTP error");
                    mainHandler.post(() -> callback.onFailure(e));
                    return;
                }

                try {
                    JSONObject json = new JSONObject(responseBody);
                    String secureUrl = json.getString("secure_url");
                    String publicId = json.optString("public_id", null);
                    android.util.Log.d("StorageRepository", "4. Parsed secure_url: " + secureUrl + ", public_id: " + publicId);
                    android.util.Log.d("StorageRepository", "5. onSuccess() callback");
                    mainHandler.post(() -> callback.onSuccess(secureUrl, publicId));
                } catch (Exception e) {
                    android.util.Log.e("StorageRepository", "6/7. onFailure() callback - JSONException/Exception: ", e);
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }
}

