package com.example.supermarket;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import models.Sale;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends AppCompatActivity {

    TextView txtCoke, txtFanta, txtSprite, txtGrand;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();
        txtCoke = findViewById(R.id.txtCokeTotal);
        txtFanta = findViewById(R.id.txtFantaTotal);
        txtSprite = findViewById(R.id.txtSpriteTotal);
        txtGrand = findViewById(R.id.txtGrandTotal);

        // Start listening to the cloud database immediately
        listenToSales();
    }

    private void listenToSales() {
        // This runs automatically whenever a change happens in the database
        db.collection("sales").addSnapshotListener((snapshots, e) -> {
            if (e != null) return;

            double cokeSum = 0, fantaSum = 0, spriteSum = 0, grandSum = 0;

            for (DocumentSnapshot doc : snapshots) {
                Sale sale = doc.toObject(Sale.class);
                if (sale != null) {
                    grandSum += sale.getAmount();

                    if ("Coke".equals(sale.getBrand())) cokeSum += sale.getAmount();
                    else if ("Fanta".equals(sale.getBrand())) fantaSum += sale.getAmount();
                    else if ("Sprite".equals(sale.getBrand())) spriteSum += sale.getAmount();
                }
            }

            // Update UI
            txtCoke.setText("Coke: " + cokeSum + " KES");
            txtFanta.setText("Fanta: " + fantaSum + " KES");
            txtSprite.setText("Sprite: " + spriteSum + " KES");
            txtGrand.setText("GRAND TOTAL: " + grandSum + " KES");
        });
    }
}