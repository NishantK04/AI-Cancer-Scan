package com.nishant.cancerprediction;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import static android.Manifest.permission.CAMERA;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class DetectCancerActivity extends AppCompatActivity {

    // ─────────────────────────── constants ───────────────────────────
    private static final int REQUEST_IMAGE_PICK      = 1;
    private static final int REQUEST_IMAGE_CAPTURE   = 2;
    private static final int REQ_CAMERA_PERMISSION   = 101;

    // ─────────────────────────── UI ───────────────────────────
    private ImageView back_button, imagePreview;
    private Button btnUpload, btnCamera, btnDetect;
    private ProgressBar progressBar;
    private TextView progress_percent, uploadingText;
    private LinearLayout bottom_section;

    // ────────────────────── data / model ─────────────────────
    private Bitmap selectedImage;
    private static final String EXTRA_IMAGE = "image_bytes";
    private Interpreter tflite;
    private String cancerType;

    // ───────────────────────── onCreate ──────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detect_cancer);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });




        // find views
        back_button      = findViewById(R.id.back_button);
        progress_percent = findViewById(R.id.progress_percent);
        imagePreview     = findViewById(R.id.image_preview);
        btnUpload        = findViewById(R.id.btn_upload);
        btnCamera        = findViewById(R.id.btn_camera);
        btnDetect        = findViewById(R.id.btn_detect);
        progressBar      = findViewById(R.id.upload_progress);
        uploadingText    = findViewById(R.id.uploading_text);
        bottom_section   = findViewById(R.id.bottom_section);

        cancerType = getIntent().getStringExtra("cancer_type");

        back_button.setOnClickListener(v -> onBackPressed());
        btnUpload .setOnClickListener(v -> pickImageFromGallery());
        btnCamera .setOnClickListener(v -> captureImageFromCamera());
        btnDetect .setOnClickListener(v -> detectCancer());

        bottom_section.setVisibility(View.GONE);



    }

    // ───────────────────────── gallery ───────────────────────
    private void pickImageFromGallery() {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("image/*");
        startActivityForResult(i, REQUEST_IMAGE_PICK);
    }

    // ─────────────────────── camera flow ─────────────────────
    private void captureImageFromCamera() {
        // 1) Check permission
        if (ContextCompat.checkSelfPermission(this, CAMERA) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{CAMERA},
                    REQ_CAMERA_PERMISSION
            );
            return;
        }
        // 2) Permission already granted → launch camera
        launchCameraIntent();
    }

    private void launchCameraIntent() {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (i.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(i, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    // ───────── handle permission result (API 23+) ──────────
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                launchCameraIntent();
            } else {
                Toast.makeText(this,
                        "Camera permission is required to take a photo",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ─────────────── onActivityResult (old API) ─────────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK || data == null) return;

        if (requestCode == REQUEST_IMAGE_PICK) {
            Uri uri = data.getData();
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                selectedImage = BitmapFactory.decodeStream(in);
                imagePreview.setImageBitmap(selectedImage);
            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
            selectedImage = (Bitmap) data.getExtras().get("data");
            imagePreview.setImageBitmap(selectedImage);
        }
    }

    // ─────────────── prediction pipeline ───────────────
    private void detectCancer() {
        if (selectedImage == null) {
            Toast.makeText(this, "Please select an image first.", Toast.LENGTH_SHORT).show();
            return;
        }

        bottom_section.setVisibility(View.VISIBLE);
        uploadingText.setText("Starting…");
        progressBar.setProgress(0);

        android.os.Handler h = new android.os.Handler();

        h.postDelayed(() -> updateProgress("Loading image…",   20),  500);
        h.postDelayed(() -> updateProgress("Initializing model…", 50), 1500);
        h.postDelayed(() -> updateProgress("Running prediction…", 80), 2500);

        h.postDelayed(() -> {
            updateProgress("Finalizing…", 100);

            try {
                // ---------- identifier ----------
                String idModel = cancerType.equals("lung")
                        ? "photoidentifiermodelforLung.tflite"
                        : "photoidentifiermodelforBreast.tflite";

                Interpreter identifier = new Interpreter(loadModelFile(idModel));
                Bitmap idBmp = Bitmap.createScaledBitmap(selectedImage, 224, 224, true);
                ByteBuffer idIn = convertBitmapToByteBuffer(idBmp);
                float[][] idOut = new float[1][1];
                identifier.run(idIn, idOut);

                boolean valid = cancerType.equals("lung")
                        ? idOut[0][0] > 0.5f   // lung: >0.5 means valid
                        : idOut[0][0] < 0.5f;  // breast: <0.5 means valid

                if (!valid) {
                    bottom_section.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Invalid image. Please select a valid CT scan image.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // ---------- detection ----------
                String model = cancerType.equals("lung") ? "lung_model.tflite" : "breast_model.tflite";
                tflite = new Interpreter(loadModelFile(model));

                Bitmap resized = Bitmap.createScaledBitmap(selectedImage, 224, 224, true);
                ByteBuffer input = convertBitmapToByteBuffer(resized);

                float[][] output = cancerType.equals("lung") ? new float[1][2] : new float[1][1];
                tflite.run(input, output);

                String label;
                float conf;

                if (cancerType.equals("lung")) {
                    int idx = output[0][0] > output[0][1] ? 0 : 1;
                    conf  = output[0][idx] * 100f;
                    label = (idx == 0) ? "Non-Cancer (Benign)" : "Cancer (Malignant)";
                } else {
                    float non = output[0][0] * 100f;
                    float can = (1f - output[0][0]) * 100f;
                    if (can > non) { label = "Cancer"; conf = can; }
                    else           { label = "Non-Cancer"; conf = non; }
                }

                Uri imageUri = saveBitmapToCache(selectedImage);

                Intent i = new Intent(this, ResultActivity.class);
                i.putExtra("confidence",   conf);
                i.putExtra("result_label", label);
                i.putExtra("cancer_type",  cancerType);
                i.putExtra("imageUri", imageUri.toString());
                startActivity(i);

            } catch (IOException e) {
                Toast.makeText(this, "Model load error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

            bottom_section.setVisibility(View.GONE);
        }, 3500);
    }

    private void updateProgress(String text, int pct) {
        uploadingText.setText(text);
        progress_percent.setText(pct + "%");
        progressBar.setProgress(pct);
    }

    // ─────────────── helpers ───────────────
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bmp) {
        ByteBuffer buf = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);
        buf.order(ByteOrder.nativeOrder());
        int[] pixels = new int[224 * 224];
        bmp.getPixels(pixels, 0, 224, 0, 0, 224, 224);

        int p = 0;
        for (int i = 0; i < 224; i++) {
            for (int j = 0; j < 224; j++) {
                int v = pixels[p++];
                buf.putFloat(((v >> 16) & 0xFF) / 255f);
                buf.putFloat(((v >>  8) & 0xFF) / 255f);
                buf.putFloat(( v        & 0xFF) / 255f);
            }
        }
        return buf;
    }

    private MappedByteBuffer loadModelFile(String name) throws IOException {
        AssetFileDescriptor fd = getAssets().openFd(name);
        FileInputStream   in = new FileInputStream(fd.getFileDescriptor());
        FileChannel       ch = in.getChannel();
        return ch.map(FileChannel.MapMode.READ_ONLY,
                fd.getStartOffset(),
                fd.getDeclaredLength());
    }

    private Uri saveBitmapToCache(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs(); // create folder if not exists
            File file = new File(cachePath, "image.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
            return Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
