package in.eatie.backgroundgeolocation;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class OkHTTP {
    OkHttpClient client = new OkHttpClient();
    private String TAG = OkHTTP.class.getSimpleName();
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");


    void post(String url, String json,String token) throws IOException {
        RequestBody body = RequestBody.create(json,JSON);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body);
        if(token !=null){
            requestBuilder.addHeader("authorization","Bearer " + token);
        }
        Request request = requestBuilder.build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                Log.e(TAG, "onFailure: " + e.toString() );
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.i(TAG, "onResponse: "+response.body().string());
            }
        });
    }
}
