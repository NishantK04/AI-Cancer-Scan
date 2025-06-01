package com.nishant.cancerprediction;

import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class dashBoardActivity extends AppCompatActivity {

    CardView btnLung;
    CardView btnBreast;
    private DrawerLayout drawerLayout;
    private View main;

    private ImageView settingIcon, userImage;
    private TextView userName, userEmail, logoutText;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));

        FloatingActionButton fab = findViewById(R.id.ai_fab);

        fab.setOnClickListener(v -> {
            // Haptic feedback
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);

            // Touch animation to visually feel the press
            v.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(100)
                    .withEndAction(() -> v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start())
                    .start();

            ChatBottomSheet sheet = new ChatBottomSheet();
            sheet.show(getSupportFragmentManager(), sheet.getTag());


        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize Views
        drawerLayout = findViewById(R.id.drawer_layout);
        settingIcon = findViewById(R.id.setting_icon);
        userImage = findViewById(R.id.userImage);
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        logoutText = findViewById(R.id.logout_text);
        main = findViewById(R.id.main);


        // Set user info
        if (currentUser != null) {
            // Set profile details
            userName.setText(currentUser.getDisplayName());
            userEmail.setText(currentUser.getEmail());

            // Use Glide to load the user's profile picture
            Glide.with(this)
                    .load(currentUser.getPhotoUrl()) // Load the URL from Firebase
                    .circleCrop() // Crop the image to a circle
                    .into(userImage); // Set the image into the ImageView
        }

        // Setting icon click to open drawer
        settingIcon.setOnClickListener(v -> drawerLayout.openDrawer(findViewById(R.id.drawer_view)));

        // Logout functionality
        logoutText.setOnClickListener(v -> {
            mAuth.signOut(); // Firebase sign out
            Intent intent = new Intent(dashBoardActivity.this, MainActivity.class); // Redirect to login
            startActivity(intent);
            finish();
        });

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        View drawerDimView = findViewById(R.id.drawer_dim);

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // Fade-in/out the dim view based on drawer offset (no translation or scaling)
                drawerDimView.setVisibility(View.VISIBLE);  // Ensure the dim view is visible
                drawerDimView.setAlpha(slideOffset);  // Fade the dim view in/out with the drawer
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Ensure dim view is fully opaque when drawer is opened
                drawerDimView.setAlpha(1f);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                // Hide the dim view and reset it when drawer is closed
                drawerDimView.setVisibility(View.GONE);
            }
        });

        btnLung = findViewById(R.id.card1);
        btnBreast = findViewById(R.id.card2);

        btnLung.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDetectCancerActivity("lung");
            }
        });

        btnBreast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDetectCancerActivity("breast");
            }
        });





    }

    private void openDetectCancerActivity(String cancerType) {
        Intent intent = new Intent(dashBoardActivity.this, DetectCancerActivity.class);
        intent.putExtra("cancer_type", cancerType);

        // 🛠️ Add this line to send correct model file name
        if (cancerType.equals("lung")) {
            intent.putExtra("model_name", "lung_model.tflite");
        } else if (cancerType.equals("breast")) {
            intent.putExtra("model_name", "breast_model.tflite");
        }
        startActivity(intent);
    }


    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(findViewById(R.id.drawer_view))) {
            drawerLayout.closeDrawer(findViewById(R.id.drawer_view));
        } else {
            super.onBackPressed();
        }
    }


}