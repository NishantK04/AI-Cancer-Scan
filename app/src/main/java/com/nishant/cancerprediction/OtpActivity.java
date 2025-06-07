package com.nishant.cancerprediction;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class OtpActivity extends AppCompatActivity {

    EditText otp1, otp2, otp3, otp4, otp5, otp6;
    TextView phone_number, resendTimerText;
    private String verificationId;
    Button verifyOtp;
    ProgressBar progressBar;

    CountDownTimer countDownTimer;
    long timeLeftInMillis = 60000; // 60 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);
        verifyOtp = findViewById(R.id.verifyOtp);
        phone_number = findViewById(R.id.phone_number);
        resendTimerText = findViewById(R.id.resend_timer); // NEW
        progressBar = findViewById(R.id.otpProgress);

        // Get data from previous activity
        String mobile = getIntent().getStringExtra("mobile_number");
        verificationId = getIntent().getStringExtra("verificationId");
        phone_number.setText(mobile);

        // OTP input focus handling
        setupOtpInputs();

        // Back
        TextView goBackText = findViewById(R.id.go_back_text);
        goBackText.setOnClickListener(v -> finish());

        // Start resend timer
        startResendOtpTimer();

        // Verify OTP logic
        verifyOtp.setOnClickListener(v -> {
            String enteredOtp = otp1.getText().toString().trim() +
                    otp2.getText().toString().trim() +
                    otp3.getText().toString().trim() +
                    otp4.getText().toString().trim() +
                    otp5.getText().toString().trim() +
                    otp6.getText().toString().trim();

            if (enteredOtp.length() == 6 && verificationId != null) {
                progressBar.setVisibility(View.VISIBLE);
                verifyOtp.setEnabled(false);

                verifyOtp.post(() -> {
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, enteredOtp);

                    FirebaseAuth.getInstance().signInWithCredential(credential)
                            .addOnCompleteListener(task -> {
                                progressBar.setVisibility(View.GONE);
                                verifyOtp.setEnabled(true);

                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(OtpActivity.this, dashBoardActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                });

            } else {
                Toast.makeText(this, "Enter valid 6-digit OTP", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupOtpInputs() {
        EditText[] otpBoxes = {otp1, otp2, otp3, otp4, otp5, otp6};

        for (int i = 0; i < otpBoxes.length; i++) {
            final int currentIndex = i;

            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && currentIndex < otpBoxes.length - 1) {
                        otpBoxes[currentIndex + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            otpBoxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_DEL &&
                        otpBoxes[currentIndex].getText().toString().isEmpty() &&
                        currentIndex > 0) {
                    otpBoxes[currentIndex - 1].requestFocus();
                }
                return false;
            });
        }
    }

    private void startResendOtpTimer() {
        resendTimerText.setEnabled(false);
        resendTimerText.setTextColor(Color.GRAY);

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                int seconds = (int) (timeLeftInMillis / 1000);
                resendTimerText.setText("Didn’t get the OTP? Resend in " + seconds + "s");
            }

            @Override
            public void onFinish() {
                resendTimerText.setText("Resend OTP");
                resendTimerText.setEnabled(true);
                resendTimerText.setTextColor(Color.WHITE);

                resendTimerText.setOnClickListener(v -> {
                    // TODO: Add your Firebase OTP resend logic here
                    Toast.makeText(OtpActivity.this, "OTP Resent!", Toast.LENGTH_SHORT).show();

                    // Restart the timer
                    timeLeftInMillis = 60000;
                    startResendOtpTimer();
                });
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
