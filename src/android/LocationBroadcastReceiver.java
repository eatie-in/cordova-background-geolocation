package in.eatie.backgroundgeolocation;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * Receiver for handling location updates.
 * <p>
 * For apps targeting API level O
 * {@link android.app.PendingIntent#getBroadcast(Context, int, Intent, int)} should be used when
 * requesting location updates. Due to limits on background services,
 * {@link android.app.PendingIntent#getService(Context, int, Intent, int)} should not be used.
 * <p>
 * Note: Apps running on "O" devices (regardless of targetSdkVersion) may receive updates
 * less frequently than the interval specified in the
 * {@link com.google.android.gms.location.LocationRequest} when the app is no longer in the
 * foreground.
 */
public class LocationBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "LUBroadcastReceiver";

    OkHTTP mOkHTTP = new OkHTTP();

    static final String ACTION_PROCESS_UPDATES =
            "in.eatie.backgroundgeolocation" +
                    ".PROCESS_UPDATES";


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_UPDATES.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    List<Location> locations = result.getLocations();
                    // debug
                    if (BackgroundGeolocation.mDebug) {
                        Utils.sendNotification(context, Utils.getLocationResultTitle(context, locations));
                    }
                    Location location = locations.get(0);
                    Log.i(TAG, "onReceive: " + location.toString());
                    parseLocation(location);
                }
            }
        }
    }

    void parseLocation(Location location) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("latitude", location.getLatitude());
            jsonObject.put("longitude", location.getLongitude());
            Log.i(TAG, jsonObject.toString());
            BackgroundGeolocation.sendData(jsonObject);
            // send response
            if (BackgroundGeolocation.mApiUrl != null) {
                // send the response to api
                post(jsonObject.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void post(String body) {
        String token = BackgroundGeolocation.mApiToken;
        String url = BackgroundGeolocation.mApiUrl;
        try {
            mOkHTTP.post(url, body, token);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
