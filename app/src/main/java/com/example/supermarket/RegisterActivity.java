package com.example.supermarket;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registers);

        // 1. Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Bind Views
        etName = findViewById(R.id.etRegName);
        etEmail = findViewById(R.id.etRegEmail);
        etPassword = findViewById(R.id.etRegPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvGoToLogin);

        // 3. Set Button Click Listener
        btnRegister.setOnClickListener(v -> performRegistration());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void performRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // --- VALIDATION CHECKS ---
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            Toast.makeText(this, "Password is too short (min 6 chars)", Toast.LENGTH_LONG).show();
            return;
        }

        // --- FIREBASE REGISTRATION ---
        Toast.makeText(this, "Attempting to register...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // SUCCESS
                        Log.d("REGISTER", "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();

                        // Go to Customer Dashboard
                        Intent intent = new Intent(RegisterActivity.this, CustomerActivity.class);
                        // Clear the back stack so they can't go back to register
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        // FAILURE - This gets the exact error message
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown Error";
                        Log.w("REGISTER", "createUserWithEmail:failure", task.getException());

                        // SHOW THE ERROR ON SCREEN
                        Toast.makeText(this, "Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}