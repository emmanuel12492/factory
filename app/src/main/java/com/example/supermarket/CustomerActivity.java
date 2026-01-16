package com.example.supermarket;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import models.Sale;
import com.google.firebase.firestore.FirebaseFirestore;

public class CustomerActivity extends AppCompatActivity {

    Spinner spBranch;
    RadioGroup rgDrinks;
    EditText etQty, etPhone;
    Button btnBuy;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        db = FirebaseFirestore.getInstance();
        spBranch = findViewById(R.id.spBranch);
        rgDrinks = findViewById(R.id.rgDrinks);
        etQty = findViewById(R.id.etQuantity);
        etPhone = findViewById(R.id.etPhone);
        btnBuy = findViewById(R.id.btnBuy);

        btnBuy.setOnClickListener(v -> processTransaction());
    }

    private void processTransaction() {
        String branch = spBranch.getSelectedItem().toString();
        int selectedId = rgDrinks.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(this, "Please select a drink", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton rb = findViewById(selectedId);
        String brand = rb.getText().toString();
        String qtyStr = etQty.getText().toString();

        if (qtyStr.isEmpty()) return;

        int qty = Integer.parseInt(qtyStr);
        double amount = qty * 100.0; // Fixed price 100 KES

        // --- M-PESA INTEGRATION NOTE ---
        // Ideally, you call your Retrofit service here.
        // For the presentation, we simulate a successful callback:

        saveSaleToFirestore(branch, brand, amount, qty);
    }

    private void saveSaleToFirestore(String branch, String brand, double amount, int qty) {
        Sale sale = new Sale(branch, brand, amount, qty);

        db.collection("sales").add(sale)
                .addOnSuccessListener(doc ->
                        Toast.makeText(this, "Payment Received & Order Sent!", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}