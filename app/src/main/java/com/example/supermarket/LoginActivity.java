package com.example.supermarket;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvRegister;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // Bind views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvGoToRegister);

        // Set click listeners
        btnLogin.setOnClickListener(v -> login());

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void login() {
        // .trim() removes accidental spaces at the start or end
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        // 1. Validation: Don't just return, tell the user what's wrong
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Attempt Firebase Login
        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();

                // 3. CHECK FOR ADMIN
                // This exact email must match what you used in RegisterActivity
                if (email.equals("admin@supermarket.com")) {
                    // Go to Admin Dashboard
                    Intent intent = new Intent(this, AdminActivity.class);
                    startActivity(intent);
                } else {
                    // Go to Customer Shop
                    Intent intent = new Intent(this, CustomerActivity.class);
                    startActivity(intent);
                }
                finish(); // Close LoginActivity so back button doesn't return here
            } else {
                // 4. Handle Errors (Wrong password, no internet, etc.)
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(this, "Login Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}