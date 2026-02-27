#### Supermarket Distributed Android System (Firebase Version)

#### Overview

This project implements a **distributed Android application** for a supermarket chain headquartered in **Nairobi** with branches in **Kisumu, Mombasa, Nakuru, and Eldoret**.  

The supermarket sells **soft drinks** (Coke, Fanta, Sprite), and all drinks are sold at the same price. Customers can purchase drinks from any branch, and restocking is done exclusively from the headquarters.  

The system includes:
- Customer registration and login
- Admin login for managing inventory
- Real-time sales reporting
- Integration with **M-Pesa Sandbox API** for payments

All backend functionality is handled via **Firebase**:
- Authentication → Firebase Auth
- Database → Firestore (or Realtime Database)
- Cloud Functions → For inventory updates, payment callbacks, and reports

---

#### Features

##### Customer Features
- Register and login
- Browse drinks by branch
- Make purchases using **M-Pesa Sandbox API**
- View purchase receipts in real-time

##### Admin Features
- Login as admin
- Restock drinks for all branches from headquarters
- View detailed sales reports:
  - Drinks sold per brand
  - Revenue per brand
  - Grand total of all sales
- Monitor branch inventory levels in real-time

#### Firebase Structure

##### Users Collection
| Field        | Type   | Description                |
|--------------|--------|----------------------------|
| uid          | string | Firebase UID               |
| name         | string | Full name                  |
| phone        | string | Phone number               |
| role         | string | `admin` or `customer`      |

### Branches Collection
| Field    | Type   | Description          |
|----------|--------|--------------------|
| id       | string | Branch document ID  |
| name     | string | Branch name        |
| location | string | City/location      |

##### Inventory Subcollection (per branch)
| Field      | Type   | Description             |
|------------|--------|-------------------------|
| drinkType  | string | Coke / Fanta / Sprite   |
| quantity   | number | Number of drinks        |

##### Sales Collection
| Field        | Type   | Description                    |
|--------------|--------|--------------------------------|
| id           | string | Document ID                     |
| userId       | string | UID of customer                 |
| branchId     | string | Branch where sale occurred      |
| drinkType    | string | Coke / Fanta / Sprite           |
| quantity     | number | Drinks purchased                |
| totalPrice   | number | Total price                     |
| mpesaReceipt | string | M-Pesa transaction ID           |
| timestamp    | timestamp | Sale time                      |

---

#### M-Pesa Payment Integration

**Flow:**
1. Customer selects branch and drinks
2. Clicks **Pay**
3. Firebase Cloud Function triggers **STK Push** via M-Pesa Sandbox
4. Customer enters M-Pesa PIN
5. Safaricom sends callback to Firebase Function
6. Inventory is updated atomically, and sale is logged in Firestore

---

## Admin Dashboard Reports

Admin can view:

1. **Total drinks sold per brand**  
2. **Revenue per brand**  
3. **Grand total of all sales**

Firestore queries can aggregate data using Firebase Functions for reporting.

---

#### Demonstration Setup

- **Device 1:** Admin (restocking + reports)
- **Devices 2–4:** Customers (making independent purchases)
- **Real-time updates:** Firestore synchronizes sales and stock across all devices instantly

---

#### Best Practices

- Use **Firebase Authentication** for secure login
- Validate stock before purchases to prevent overselling
- Handle M-Pesa callback atomically in Cloud Functions
- Log all transactions for audit and debugging
- Protect admin functionalities using role-based access

---

#### Future Enhancements

- Real-time analytics dashboard
- Sales breakdown per branch
- Daily/weekly automated sales reports
- CSV export for accounting
- AI-powered sales prediction and inventory optimization

---
