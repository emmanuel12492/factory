package network;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class MpesaService {

    // 1. The Retrofit Instance
    private static Retrofit retrofit = null;

    public static MpesaApi getApi() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://sandbox.safaricom.co.ke/") // Sandbox URL
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(MpesaApi.class);
    }

    // 2. The Interface defining the API endpoints
    public interface MpesaApi {

        // Endpoint to get the Access Token (Auth)
        @GET("oauth/v1/generate?grant_type=client_credentials")
        @Headers("Authorization: Basic [YOUR_BASE64_KEY_HERE]")
        Call<AccessTokenResponse> getAccessToken();

        // Endpoint to trigger the STK Push
        @POST("mpesa/stkpush/v1/processrequest")
        @Headers("Content-Type: application/json")
        Call<StkPushResponse> performStkPush(@Body StkPushPayload payload, @retrofit2.http.Header("Authorization") String token);
    }

    // --- INNER CLASSES FOR DATA MODELS (To keep it in one file for you) ---

    // Model for the STK Push Data we send
    public static class StkPushPayload {
        public String BusinessShortCode;
        public String Password;
        public String Timestamp;
        public String TransactionType;
        public String Amount;
        public String PartyA;
        public String PartyB;
        public String PhoneNumber;
        public String CallBackURL;
        public String AccountReference;
        public String TransactionDesc;

        public StkPushPayload(String shortCode, String pass, String time, String amount, String phone) {
            this.BusinessShortCode = shortCode;
            this.Password = pass;
            this.Timestamp = time;
            this.TransactionType = "CustomerPayBillOnline";
            this.Amount = amount;
            this.PartyA = phone;
            this.PartyB = shortCode;
            this.PhoneNumber = phone;
            this.CallBackURL = "https://mydomain.com/path"; // Dummy URL is fine for Sandbox
            this.AccountReference = "Supermarket";
            this.TransactionDesc = "Drink Payment";
        }
    }

    // Model for the Token Response we receive
    public static class AccessTokenResponse {
        public String access_token;
        public String expires_in;
    }

    // Model for the STK Push Response we receive
    public static class StkPushResponse {
        public String MerchantRequestID;
        public String CheckoutRequestID;
        public String ResponseCode;
        public String ResponseDescription;
        public String CustomerMessage;
    }
}