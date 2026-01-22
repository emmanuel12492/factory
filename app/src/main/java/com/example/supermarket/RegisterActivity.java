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

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registers);

        mAuth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.etRegName);
        etEmail = findViewById(R.id.etRegEmail);
        etPassword = findViewById(R.id.etRegPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvGoToLogin);

        btnRegister.setOnClickListener(v -> performRegistration());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void performRegistration() {
        String email = etEmail.getText().toString().trim(); // Trim is important!
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();

                        // --- NEW LOGIC ADDED HERE ---
                        // Check if this new user is the Admin
                        if (email.equals("admin@supermarket.com")) {
                            // Go to Admin Dashboard
                            startActivity(new Intent(RegisterActivity.this, AdminActivity.class));
                        } else {
                            // Go to Customer Dashboard
                            startActivity(new Intent(RegisterActivity.this, CustomerActivity.class));
                        }

                        finish();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Error";
                        Toast.makeText(this, "Registration Failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}