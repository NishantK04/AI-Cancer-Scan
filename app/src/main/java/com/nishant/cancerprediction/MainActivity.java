package com.nishant.cancerprediction;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.hbb20.CountryCodePicker;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    CountryCodePicker ccp;
    EditText editTextPhone;
    ProgressBar progressBar;
    Button loginButton;
    ImageButton googleSignInButton;
    String selectedCountryCode = "";

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Setup status bar color
        getWindow().setStatusBarColor(getResources().getColor(R.color.blueforstatus));

        // Initialize UI components
        progressBar = findViewById(R.id.progressBar);
        ccp = findViewById(R.id.ccp);
        editTextPhone = findViewById(R.id.editTextPhone);
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleButton); // Add this in your XML

        // Google Sign-In setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.DEFAULT_WEB_CLIENT_ID)  // Make sure to replace with your web client ID
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Google Sign-In Button click
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        // Set initial country code
        selectedCountryCode = ccp.getSelectedCountryCodeWithPlus() + " ";
        setPhoneWithCountryCode();

        // Update phone field when country code changes
        ccp.setOnCountryChangeListener(() -> {
            selectedCountryCode = ccp.getSelectedCountryCodeWithPlus() + " ";
            setPhoneWithCountryCode();
        });

        // Ensure country code remains at the start
        editTextPhone.addTextChangedListener(new TextWatcher() {
            boolean isEditing = false;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;

                if (!s.toString().startsWith(selectedCountryCode)) {
                    String digitsOnly = s.toString().replaceAll("[^\\d]", "");
                    String phoneNumberOnly = digitsOnly.replaceFirst("^" + selectedCountryCode.replace("+", "").trim(), "");
                    editTextPhone.setText(selectedCountryCode + phoneNumberOnly);
                    editTextPhone.setSelection(editTextPhone.getText().length());
                }

                isEditing = false;
            }
        });

        // Phone Number Login Button Clicked
        loginButton.setOnClickListener(v -> {
            String mobile = editTextPhone.getText().toString().replaceAll("\\s+", "");

            if (mobile.isEmpty() || mobile.length() < 10) {
                Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);

            // Start phone number verification
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                    .setPhoneNumber(mobile)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                            // Optional: Auto verification
                            Toast.makeText(MainActivity.this, "Verification complete", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
                            progressBar.setVisibility(View.GONE);
                            loginButton.setEnabled(true);
                            Toast.makeText(getApplicationContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onCodeSent(@NonNull String verificationId,
                                               @NonNull PhoneAuthProvider.ForceResendingToken token) {
                            progressBar.setVisibility(View.GONE);
                            Intent intent = new Intent(MainActivity.this, OtpActivity.class);
                            intent.putExtra("verificationId", verificationId);
                            intent.putExtra("mobile_number", mobile);
                            startActivity(intent);
                        }
                    })
                    .build();

            PhoneAuthProvider.verifyPhoneNumber(options);
        });
    }

    private void setPhoneWithCountryCode() {
        editTextPhone.setText(selectedCountryCode);
        editTextPhone.setSelection(editTextPhone.getText().length());
    }

    // Google Sign-In method
    private void signInWithGoogle() {
        progressBar.setVisibility(View.VISIBLE);
        googleSignInButton.setEnabled(false);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            GoogleSignInAccount account = task.getResult();
                            firebaseAuthWithGoogle(account);
                        } else {
                            Toast.makeText(MainActivity.this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            googleSignInButton.setEnabled(true);
                        }
                    });
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseAuth.getInstance()
                .signInWithCredential(GoogleAuthProvider.getCredential(account.getIdToken(), null))
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Sign-in successful, go to Dashboard
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        Intent intent = new Intent(MainActivity.this, dashBoardActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // If already signed in, go directly to Dashboard
            Intent intent = new Intent(MainActivity.this, dashBoardActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
