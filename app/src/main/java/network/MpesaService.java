package network;

import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class MpesaService {

    // --- YOUR KEYS ---
    private static final String CONSUMER_KEY = "R9AKA7JYdt7YNLNflzvyTJFXpjmzaicLqgpTaHvF8dGdz5Je";
    private static final String CONSUMER_SECRET = "fkfg1G4B0xaA0SDPYirGCjj0PsJwhSSYUp3TRG52tQPwWoFvMjQiG8tcBH25Eluv";

    private static final String BUSINESS_SHORT_CODE = "174379";
    private static final String PASSKEY = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919";

    private static Retrofit retrofit = null;
    private static MpesaApi api = null;

    public static MpesaApi getApi() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://sandbox.safaricom.co.ke/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            api = retrofit.create(MpesaApi.class);
        }
        return api;
    }

    // --- 1. SEND PUSH ---
    public static void triggerStkPush(String phoneNumber, String amount, final MpesaListener listener) {
        String cleanKey = CONSUMER_KEY.trim();
        String cleanSecret = CONSUMER_SECRET.trim();
        String keys = cleanKey + ":" + cleanSecret;
        String authHeader = "Basic " + Base64.encodeToString(keys.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

        getApi().getAccessToken(authHeader).enqueue(new retrofit2.Callback<AccessTokenResponse>() {
            @Override
            public void onResponse(Call<AccessTokenResponse> call, retrofit2.Response<AccessTokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sendActualPush(response.body().access_token, phoneNumber, amount, listener);
                } else {
                    listener.onError("Auth Failed: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<AccessTokenResponse> call, Throwable t) {
                listener.onError("Network Error: " + t.getMessage());
            }
        });
    }

    private static void sendActualPush(String token, String phone, String amount, final MpesaListener listener) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        String password = Base64.encodeToString((BUSINESS_SHORT_CODE + PASSKEY + timestamp).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

        StkPushPayload payload = new StkPushPayload(BUSINESS_SHORT_CODE, password, timestamp, amount, phone);

        getApi().performStkPush(payload, "Bearer " + token).enqueue(new retrofit2.Callback<StkPushResponse>() {
            @Override
            public void onResponse(Call<StkPushResponse> call, retrofit2.Response<StkPushResponse> response) {
                if (response.isSuccessful()) {
                    // PASS THE CHECKOUT ID BACK SO WE CAN TRACK IT
                    listener.onSuccess(response.body().CheckoutRequestID);
                } else {
                    listener.onError("Push Failed: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<StkPushResponse> call, Throwable t) {
                listener.onError("Error: " + t.getMessage());
            }
        });
    }

    // --- 2. CHECK STATUS (NEW) ---
    public static void checkTransactionStatus(String checkoutRequestId, final StatusListener listener) {
        String cleanKey = CONSUMER_KEY.trim();
        String cleanSecret = CONSUMER_SECRET.trim();
        String keys = cleanKey + ":" + cleanSecret;
        String authHeader = "Basic " + Base64.encodeToString(keys.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

        getApi().getAccessToken(authHeader).enqueue(new retrofit2.Callback<AccessTokenResponse>() {
            @Override
            public void onResponse(Call<AccessTokenResponse> call, retrofit2.Response<AccessTokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    performStatusQuery(response.body().access_token, checkoutRequestId, listener);
                } else {
                    listener.onResult("ERROR", "Auth Failed");
                }
            }
            @Override
            public void onFailure(Call<AccessTokenResponse> call, Throwable t) {
                listener.onResult("ERROR", t.getMessage());
            }
        });
    }

    private static void performStatusQuery(String token, String checkoutId, final StatusListener listener) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        String password = Base64.encodeToString((BUSINESS_SHORT_CODE + PASSKEY + timestamp).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

        StatusQuery query = new StatusQuery(BUSINESS_SHORT_CODE, password, timestamp, checkoutId);

        getApi().queryTransaction(query, "Bearer " + token).enqueue(new retrofit2.Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, retrofit2.Response<StatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // ResultCode "0" means Success. Anything else is fail/pending.
                    if ("0".equals(response.body().ResultCode)) {
                        listener.onResult("SUCCESS", "Payment Confirmed");
                    } else {
                        listener.onResult("FAIL", "Payment Incomplete or Cancelled");
                    }
                } else {
                    listener.onResult("WAITING", "Processing... Try again in 5s");
                }
            }
            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                listener.onResult("ERROR", t.getMessage());
            }
        });
    }

    // --- INTERFACES & MODELS ---

    public interface MpesaListener {
        void onSuccess(String checkoutRequestId); // Now returns ID
        void onError(String error);
    }

    public interface StatusListener {
        void onResult(String status, String message); // status: SUCCESS, FAIL, or WAITING
    }

    public interface MpesaApi {
        @GET("oauth/v1/generate?grant_type=client_credentials")
        Call<AccessTokenResponse> getAccessToken(@Header("Authorization") String authHeader);

        @POST("mpesa/stkpush/v1/processrequest")
        @Headers("Content-Type: application/json")
        Call<StkPushResponse> performStkPush(@Body StkPushPayload payload, @Header("Authorization") String token);

        @POST("mpesa/stkpushquery/v1/query")
        @Headers("Content-Type: application/json")
        Call<StatusResponse> queryTransaction(@Body StatusQuery query, @Header("Authorization") String token);
    }

    // Models
    public static class StkPushPayload {
        public String BusinessShortCode, Password, Timestamp, TransactionType, Amount, PartyA, PartyB, PhoneNumber, CallBackURL, AccountReference, TransactionDesc;
        public StkPushPayload(String shortCode, String pass, String time, String amount, String phone) {
            this.BusinessShortCode = shortCode; this.Password = pass; this.Timestamp = time; this.TransactionType = "CustomerPayBillOnline";
            this.Amount = amount; this.PartyA = phone; this.PartyB = shortCode; this.PhoneNumber = phone;
            this.CallBackURL = "https://mydomain.com/path"; this.AccountReference = "Shop"; this.TransactionDesc = "Pay";
        }
    }
    public static class StatusQuery {
        public String BusinessShortCode, Password, Timestamp, CheckoutRequestID;
        public StatusQuery(String shortCode, String pass, String time, String checkoutId) {
            this.BusinessShortCode = shortCode; this.Password = pass; this.Timestamp = time; this.CheckoutRequestID = checkoutId;
        }
    }

    public static class AccessTokenResponse { public String access_token; }
    public static class StkPushResponse { public String CheckoutRequestID; } // We need this ID
    public static class StatusResponse { public String ResultCode; public String ResultDesc; }
}