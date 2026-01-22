package com.example.supermarket;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import network.MpesaService;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import models.Sale;

public class CustomerActivity extends AppCompatActivity {

    Spinner spBranch;
    RadioGroup rgDrinks;
    EditText etQty, etPhone, etCustomerName;
    Button btnBuy;
    FirebaseFirestore db;

    // We store the ID of the current transaction here
    private String currentCheckoutRequestId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        db = FirebaseFirestore.getInstance();
        spBranch = findViewById(R.id.spBranch);
        rgDrinks = findViewById(R.id.rgDrinks);
        etQty = findViewById(R.id.etQuantity);
        etPhone = findViewById(R.id.etPhone);
        etCustomerName = findViewById(R.id.etCustomerName);
        btnBuy = findViewById(R.id.btnBuy);

        btnBuy.setOnClickListener(v -> processTransaction());
    }

    private void processTransaction() {
        String branch = spBranch.getSelectedItem().toString();
        int selectedId = rgDrinks.getCheckedRadioButtonId();
        if (selectedId == -1) { Toast.makeText(this, "Select a drink", Toast.LENGTH_SHORT).show(); return; }

        RadioButton rb = findViewById(selectedId);
        String brand = rb.getText().toString();
        String qtyStr = etQty.getText().toString();
        if (qtyStr.isEmpty()) { etQty.setError("Enter quantity"); return; }

        String phone = etPhone.getText().toString().trim();
        if (phone.isEmpty()) { etPhone.setError("Phone required"); return; }
        if (phone.startsWith("0")) phone = "254" + phone.substring(1);

        int qty = Integer.parseInt(qtyStr);
        double totalAmount = qty * 1.0;
        String amountToSend = String.valueOf((int)totalAmount);

        Toast.makeText(this, "Sending Prompt...", Toast.LENGTH_SHORT).show();

        // TRIGGER MPESA
        MpesaService.triggerStkPush(phone, amountToSend, new MpesaService.MpesaListener() {
            @Override
            public void onSuccess(String checkoutRequestId) {
                // 1. SAVE THE ID
                currentCheckoutRequestId = checkoutRequestId;

                runOnUiThread(() -> {
                    // 2. SHOW DIALOG
                    showPaymentConfirmationDialog(branch, brand, qty, totalAmount);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(CustomerActivity.this, "Failed: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showPaymentConfirmationDialog(String branch, String brand, int qty, double amount) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirm Payment")
                .setMessage("Please enter your PIN on your phone.\n\nOnce you receive the SMS confirmation, click 'VERIFY PAYMENT'.")
                .setCancelable(false)
                .setPositiveButton("VERIFY PAYMENT", null) // Set null here so we can override the click behavior later
                .setNegativeButton("CANCEL", (d, w) -> Toast.makeText(CustomerActivity.this, "Order Cancelled", Toast.LENGTH_SHORT).show())
                .create();

        dialog.show();

        // OVERRIDE THE BUTTON CLICK (To prevent dialog closing if payment isn't ready)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (currentCheckoutRequestId == null) return;

            Toast.makeText(CustomerActivity.this, "Checking status...", Toast.LENGTH_SHORT).show();

            // 3. ASK SAFARICOM: "DID THEY PAY?"
            MpesaService.checkTransactionStatus(currentCheckoutRequestId, new MpesaService.StatusListener() {
                @Override
                public void onResult(String status, String message) {
                    runOnUiThread(() -> {
                        if (status.equals("SUCCESS")) {
                            // REAL SUCCESS!
                            dialog.dismiss();
                            checkStockAndBuy(branch, brand, qty, amount);
                        } else if (status.equals("WAITING")) {
                            // User is too slow or Safaricom is processing
                            Toast.makeText(CustomerActivity.this, "Payment processing... Wait 5s and click Verify again.", Toast.LENGTH_LONG).show();
                        } else {
                            // Failed or Cancelled
                            Toast.makeText(CustomerActivity.this, "Payment Failed/Cancelled. Please try again.", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                    });
                }
            });
        });
    }

    // --- EXISTING STOCK/SALE LOGIC ---
    private void checkStockAndBuy(String branch, String brand, int qtyToBuy, double amount) {
        db.collection("branches").document(branch).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long currentStock = documentSnapshot.getLong("stock");
                        if (currentStock == null) currentStock = 0L;
                        if (currentStock >= qtyToBuy) {
                            updateStockInFirestore(branch, currentStock - qtyToBuy, brand, amount, qtyToBuy);
                        } else {
                            Toast.makeText(this, "Order Failed: Only " + currentStock + " items left.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void updateStockInFirestore(String branch, long newStock, String brand, double amount, int qty) {
        db.collection("branches").document(branch).update("stock", newStock)
                .addOnSuccessListener(aVoid -> saveSaleToFirestore(branch, brand, amount, qty));
    }

    private void saveSaleToFirestore(String branch, String brand, double amount, int qty) {
        Sale sale = new Sale(branch, brand, amount, qty);
        db.collection("sales").add(sale).addOnSuccessListener(doc -> {
            Toast.makeText(this, "Payment Verified & Order Placed!", Toast.LENGTH_LONG).show();

            String name = etCustomerName.getText().toString();
            if (name.isEmpty()) name = "Valued Customer";
            generatePdfReceipt(name, brand, qty, amount);

            etQty.setText(""); etPhone.setText(""); rgDrinks.clearCheck();
        });
    }

    // --- UPDATED: PROFESSIONAL PDF GENERATOR ---
    private void generatePdfReceipt(String customerName, String item, int qty, double amount) {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint rightAlignPaint = new Paint();

        // 1. Setup Page (A6 size is better for receipts: 300 x 500)
        PdfDocument.PageInfo myPageInfo = new PdfDocument.PageInfo.Builder(300, 500, 1).create();
        PdfDocument.Page myPage = pdfDocument.startPage(myPageInfo);
        Canvas canvas = myPage.getCanvas();

        // 2. Setup Fonts & Colors
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTextSize(20);
        titlePaint.setFakeBoldText(true);

        paint.setTextSize(12);
        paint.setColor(Color.BLACK);

        rightAlignPaint.setTextAlign(Paint.Align.RIGHT); // NEW: For prices on the right side
        rightAlignPaint.setTextSize(12);
        rightAlignPaint.setColor(Color.BLACK);

        int pageWidth = 300;
        int leftMargin = 20;
        int rightMargin = 280; // Width (300) - Margin (20)
        int currentY = 50;

        // 3. Draw Header
        canvas.drawText("FRESH MART", pageWidth / 2, currentY, titlePaint);
        currentY += 20;
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Official Receipt", pageWidth / 2, currentY, paint);

        // Draw Line
        currentY += 15;
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint);
        currentY += 20;

        // 4. Draw Details (Left vs Right Alignment)
        paint.setTextAlign(Paint.Align.LEFT); // Reset to Left

        // Date
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Date:", leftMargin, currentY, paint);
        canvas.drawText(date, rightMargin, currentY, rightAlignPaint);
        currentY += 20;

        // Customer
        canvas.drawText("Customer:", leftMargin, currentY, paint);
        canvas.drawText(customerName, rightMargin, currentY, rightAlignPaint);
        currentY += 30; // Extra space

        // Item Details Header
        paint.setFakeBoldText(true);
        canvas.drawText("Description", leftMargin, currentY, paint);
        canvas.drawText("Price", rightMargin, currentY, rightAlignPaint);
        paint.setFakeBoldText(false);

        currentY += 10;
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint); // Line
        currentY += 20;

        // Item Logic
        canvas.drawText(item + " (x" + qty + ")", leftMargin, currentY, paint);
        canvas.drawText(amount + "0", rightMargin, currentY, rightAlignPaint); // "100.00"
        currentY += 40;

        // 5. Draw Total (Bold & Big)
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint); // Line
        currentY += 25;

        titlePaint.setTextSize(16); // Smaller title font for Total
        canvas.drawText("TOTAL PAID", leftMargin + 40, currentY, titlePaint);

        titlePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("KES " + (int)amount, rightMargin, currentY, titlePaint);

        // 6. Footer
        currentY += 50;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(10);
        paint.setColor(Color.GRAY);
        canvas.drawText("Thank you for shopping with us!", pageWidth / 2, currentY, paint);

        // 7. Finish & Save
        pdfDocument.finishPage(myPage);

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Receipt_" + System.currentTimeMillis() + ".pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "Saved to: " + file.getPath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        pdfDocument.close();
    }

}