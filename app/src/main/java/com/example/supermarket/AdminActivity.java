package com.example.supermarket;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    FirebaseFirestore db;

    // UI Variables
    TextView tvStockList, tvSalesReport;
    Spinner spBranch;
    EditText etQty;
    Button btnRestock, btnViewReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();

        // 1. Bind Views
        tvStockList = findViewById(R.id.tvStockList);
        tvSalesReport = findViewById(R.id.tvSalesReport);
        spBranch = findViewById(R.id.spRestockBranch);
        etQty = findViewById(R.id.etRestockQty);
        btnRestock = findViewById(R.id.btnRestock);
        btnViewReport = findViewById(R.id.btnViewReport);

        // 2. Load Real-time Stock Data
        loadStockLevels();

        // 3. Set Restock Button Action
        if (btnRestock != null) {
            btnRestock.setOnClickListener(v -> {
                if (spBranch.getSelectedItem() == null) return;
                String branchName = spBranch.getSelectedItem().toString();
                String qtyStr = etQty.getText().toString();

                if (!qtyStr.isEmpty()) {
                    int qtyToAdd = Integer.parseInt(qtyStr);
                    restockBranch(branchName, qtyToAdd);
                } else {
                    etQty.setError("Enter Quantity");
                }
            });
        }

        // 4. Set Report Button Action (NEW)
        if (btnViewReport != null) {
            btnViewReport.setOnClickListener(v -> generateSalesReport());
        }
    }

    // --- FUNCTION 1: GENERATE SALES REPORT ---
    // --- CORRECTED REPORT GENERATION ---
    // --- UPDATED: BRANCH-SPECIFIC REPORT ---
    private void generateSalesReport() {
        // 1. Get the branch the Admin selected in the dropdown
        if (spBranch.getSelectedItem() == null) return;
        String selectedBranch = spBranch.getSelectedItem().toString();

        tvSalesReport.setText("Calculating report for " + selectedBranch + "...");

        // 2. FILTER: Only get sales where 'branch' matches the selection
        db.collection("sales")
                .whereEqualTo("branch", selectedBranch) // <--- THIS IS THE FIX
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvSalesReport.setText("No sales found for " + selectedBranch);
                        return;
                    }

                    // 3. Reset counters
                    int cokeQty = 0, fantaQty = 0, spriteQty = 0;
                    double cokeIncome = 0, fantaIncome = 0, spriteIncome = 0;
                    double grandTotal = 0;

                    // 4. Loop through filtered sales
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String brand = doc.getString("brand");
                        Double amount = doc.getDouble("amount");
                        Long qtyLong = doc.getLong("quantity"); // Using the correct key 'quantity'

                        if (brand != null && amount != null && qtyLong != null) {
                            int qty = qtyLong.intValue();
                            grandTotal += amount;

                            if (brand.equalsIgnoreCase("Coke")) {
                                cokeQty += qty;
                                cokeIncome += amount;
                            } else if (brand.equalsIgnoreCase("Fanta")) {
                                fantaQty += qty;
                                fantaIncome += amount;
                            } else if (brand.equalsIgnoreCase("Sprite")) {
                                spriteQty += qty;
                                spriteIncome += amount;
                            }
                        }
                    }

                    // 5. Build the Report Text
                    StringBuilder report = new StringBuilder();
                    report.append("=== REPORT: " + selectedBranch.toUpperCase() + " ===\n\n");

                    report.append(String.format("COKE:\n   Sold: %d\n   Income: %.0f KES\n\n", cokeQty, cokeIncome));
                    report.append(String.format("FANTA:\n   Sold: %d\n   Income: %.0f KES\n\n", fantaQty, fantaIncome));
                    report.append(String.format("SPRITE:\n  Sold: %d\n   Income: %.0f KES\n\n", spriteQty, spriteIncome));

                    report.append("---------------------\n");
                    report.append(String.format("TOTAL INCOME: %.0f KES", grandTotal));

                    tvSalesReport.setText(report.toString());

                })
                .addOnFailureListener(e ->
                        tvSalesReport.setText("Error loading report: " + e.getMessage())
                );
    }

    // --- FUNCTION 2: LOAD STOCK LEVELS ---
    private void loadStockLevels() {
        if (tvStockList == null) return;

        db.collection("branches").addSnapshotListener((value, error) -> {
            if (error != null) {
                tvStockList.setText("Error loading data.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Current Stock Levels:\n");

            if (value != null) {
                for (DocumentSnapshot doc : value.getDocuments()) {
                    String name = doc.getId();
                    Long stock = doc.getLong("stock");
                    if (stock == null) stock = 0L;
                    sb.append("- ").append(name).append(": ").append(stock).append("\n");
                }
            }
            tvStockList.setText(sb.toString());
        });
    }

    // --- FUNCTION 3: RESTOCK BRANCH ---
    private void restockBranch(String branchName, int quantityToAdd) {
        db.collection("branches").document(branchName).get()
                .addOnSuccessListener(documentSnapshot -> {
                    long currentStock = 0;
                    if (documentSnapshot.exists()) {
                        Long val = documentSnapshot.getLong("stock");
                        if (val != null) currentStock = val;
                    }

                    long newStock = currentStock + quantityToAdd;

                    // Update Firestore
                    Map<String, Object> update = new HashMap<>();
                    update.put("stock", newStock);

                    db.collection("branches").document(branchName).set(update)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Restocked " + branchName + " (New Total: " + newStock + ")", Toast.LENGTH_SHORT).show();
                                etQty.setText("");
                            });
                });
    }
}