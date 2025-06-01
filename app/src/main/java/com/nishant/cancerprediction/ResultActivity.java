package com.nishant.cancerprediction;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class ResultActivity extends AppCompatActivity {

    ImageView back_button;

    private TextView resultRisk, resultDescription, confidencePercent, modelUsedValue, resultPrediction;
    private ProgressBar confidenceProgress;

    private Uri imageUri;

    Button btnlearn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String imageUriStr = getIntent().getStringExtra("imageUri");
        if (imageUriStr != null) {
            imageUri = Uri.parse(imageUriStr);

        }

        back_button = findViewById(R.id.back_button);
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go back to the previous screen
                onBackPressed();  // or finish(); depending on your needs
            }
        });

        // Bind views
        resultRisk = findViewById(R.id.result_risk);
        resultDescription = findViewById(R.id.result_description);
        confidencePercent = findViewById(R.id.confidence_percent);
        modelUsedValue = findViewById(R.id.model_used_value);
        confidenceProgress = findViewById(R.id.confidence_progress);
        resultPrediction = findViewById(R.id.result_prediction);

        // Get intent data
        String resultLabel = getIntent().getStringExtra("result_label");
        float confidence = getIntent().getFloatExtra("confidence", 0f);
        String tempCancerType = getIntent().getStringExtra("cancer_type");
        if (tempCancerType == null) tempCancerType = "Unknown";
        final String cancerType = tempCancerType;

        // Fallback if data is missing
        if (resultLabel == null) resultLabel = "Unknown";

        // Set values
        confidencePercent.setText(String.format("%.2f%%", confidence)); // Show confidence as percentage
        confidenceProgress.setProgress((int) (confidence * 100)); // Update progress bar
        modelUsedValue.setText(cancerType.equals("lung") ? "Lung Cancer" : "Breast Cancer");
        resultPrediction.setText("Prediction: " + resultLabel);

        // Set risk level and description
        resultRisk.setText(resultLabel);

        // Adjust logic based on confidence value
        if ("Cancer".equals(resultLabel)) {
            // Confidence logic: Adjust the thresholds for high, moderate, and low
            if (confidence > 0.75) { // 75% and above is high confidence
                resultDescription.setText("High confidence in cancer detection.\n\nPlease consult a doctor immediately for further evaluation.");
            } else if (confidence > 0.50) { // 50% to 75% is moderate confidence
                resultDescription.setText("Moderate confidence in cancer detection.\n\nIt is advised to consult a medical professional.");
            } else { // Below 50% is low confidence
                resultDescription.setText("Low confidence, but potential signs of cancer present.\n\nRegular monitoring and check-ups are recommended.");
            }
        } else {
            resultDescription.setText("The model did not detect signs of cancer.\n\nMaintain healthy habits and go for regular screenings.");
        }

        btnlearn = findViewById(R.id.btn_learn);

        btnlearn.setOnClickListener(v -> {
            if (imageUri == null) {
                Toast.makeText(ResultActivity.this, "Image not found. Cannot proceed.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent;
            if ("lung".equalsIgnoreCase(cancerType)) {
                intent = new Intent(ResultActivity.this, HeapMapActivity.class);
            } else {
                intent = new Intent(ResultActivity.this, HeapMap2Activity.class);
            }
            intent.putExtra("image_uri", imageUri.toString());
            startActivity(intent);
        });






    }
}
