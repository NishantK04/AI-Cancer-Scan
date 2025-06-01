package com.nishant.cancerprediction;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HeapMapActivity extends AppCompatActivity {

    private ImageView gradCamImageView;
    private ProgressBar progressBar;
    private Uri imageUri;
    private ImageView btnZoom;
    private ImageView btnBack;

    private static final String FLASK_SERVER_IP = "192.168.0.190:5000";
    private static final String FLASK_POST_URL = "http://" + FLASK_SERVER_IP + "/gradcam";
    private static final String FLASK_IMAGE_URL = "http://" + FLASK_SERVER_IP + "/gradcam-image";

    private static final String TAG = "HeapMapActivity";
    private boolean isZoomedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_heap_map);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        gradCamImageView = findViewById(R.id.photoView);
        progressBar = findViewById(R.id.progressBar);
        btnZoom = findViewById(R.id.btnZoom);
        btnBack = findViewById(R.id.btnBack);
        progressBar.bringToFront();

        btnBack.setOnClickListener(v -> onBackPressed());

        btnZoom.setOnClickListener(v -> {
            float scale = isZoomedIn ? 1f : 2f;
            gradCamImageView.animate().scaleX(scale).scaleY(scale).setDuration(250).start();
            isZoomedIn = !isZoomedIn;
        });

        String imageUriStr = getIntent().getStringExtra("image_uri");
        if (imageUriStr != null) {
            imageUri = Uri.parse(imageUriStr);
            uploadImageToFlaskServer(imageUri);
        } else {
            Toast.makeText(this, "No image URI provided.", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageToFlaskServer(Uri uri) {
        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                throw new IOException("Failed to open image input stream.");
            }
            byte[] imageBytes = getBytes(inputStream);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "image.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                    .build();

            OkHttpClient client = new OkHttpClient.Builder()
                    .callTimeout(java.time.Duration.ofSeconds(30))
                    .build();

            Request request = new Request.Builder()
                    .url(FLASK_POST_URL)
                    .post(requestBody)
                    .build();

            Log.d(TAG, "Uploading image to Flask server...");

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Upload failed: " + e.getMessage());
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(HeapMapActivity.this, "Upload failed. Please try again.", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBodyStr = response.body().string();
                        Log.d(TAG, "Upload successful, response: " + responseBodyStr);

                        try {
                            JSONObject json = new JSONObject(responseBodyStr);
                            int prediction = json.getInt("prediction");

                            if (prediction == 1) {
                                fetchGradCamImage();
                            } else {
                                runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(HeapMapActivity.this,
                                            "Prediction: No Cancer Detected.\nNo heatmap needed.",
                                            Toast.LENGTH_LONG).show();
                                });
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(HeapMapActivity.this, "Error: Couldn't understand server response.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(HeapMapActivity.this, "Server error. Try again later.", Toast.LENGTH_SHORT).show();
                        });
                    }
                    response.close();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error uploading image: " + e.getMessage());
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Something went wrong.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchGradCamImage() {
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(java.time.Duration.ofSeconds(30))
                .build();

        Request request = new Request.Builder()
                .url(FLASK_IMAGE_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(HeapMapActivity.this, "Could not retrieve heatmap image.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    byte[] bytes = response.body().bytes();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                    runOnUiThread(() -> {
                        gradCamImageView.setImageBitmap(bitmap);
                        animateFadeIn(gradCamImageView);
                        progressBar.setVisibility(View.GONE);
                    });
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(HeapMapActivity.this, "Image not found on server.", Toast.LENGTH_SHORT).show();
                    });
                }
                response.close();
            }
        });
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void animateFadeIn(View view) {
        Animation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(400);
        view.startAnimation(fadeIn);
    }
}
