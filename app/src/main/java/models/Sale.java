package models;

public class Sale {
    private String branch;
    private String brand;
    private double amount;
    private int quantity;
    private long timestamp;

    public Sale() {} // Firestore needs this empty constructor

    public Sale(String branch, String brand, double amount, int quantity) {
        this.branch = branch;
        this.brand = brand;
        this.amount = amount;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }

    public String getBranch() { return branch; }
    public String getBrand() { return brand; }
    public double getAmount() { return amount; }
    public int getQuantity() { return quantity; }
}