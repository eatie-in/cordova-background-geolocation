package in.eatie.backgroundgeolocation;



import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * This class echoes a string called from JavaScript.
 */
public class BackgroundGeolocation extends CordovaPlugin {

    private static final String TAG = BackgroundGeolocation.class.getSimpleName();
    private static long UPDATE_INTERVAL = 60000; // Every 60 seconds.
    private static long FASTEST_UPDATE_INTERVAL = 30000; // Every 30 seconds
    private static long MAX_WAIT_TIME = UPDATE_INTERVAL * 5; // Every 5 minutes.
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;

    public static Activity mActivity;
    public static Context mContext;
    static BackgroundGeolocation instance;
    static CallbackContext mLocationCallbackContext;

    public static String mApiUrl;
    public static String mApiToken;
    public static Boolean mDebug = false;

    public static CallbackContext mCallbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.i(TAG, "pluginInitialize:");
        instance = this;
        mActivity = cordova.getActivity();
        mContext = mActivity.getApplicationContext();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        createLocationRequest();
    }


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("on")) {
            mLocationCallbackContext = callbackContext;
            return true;
        }else{
            mCallbackContext = callbackContext;
            if (action.equals("start")) {
                start();
                return true;
            }
            if (action.equals("stop")) {
                stop();
                return true;
            }
            if (action.equals("configure")) {
                configure(args);
                return true;
            }
            if (action.equals("checkPermissions")) {
                checkPermissions();
                return true;
            }
            if (action.equals("requestPermissions")) {
                requestPermissions();
                return true;
            }
        }
        return false;
    }

    public void start() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Boolean hasPermissions = Utils.checkPermissions(mContext);
                if (hasPermissions) {
                    requestLocationUpdates();
                } else {
                    Utils.requestPermissions(mActivity);
                }
            }
        });
    }

    public void stop() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                removeLocationUpdates();
            }
        });
    }

    public void configure(final JSONArray args) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject options = args.getJSONObject(0);
                    if (options.has("debug")) {
                        mDebug = options.getBoolean("debug");
                    }
                    if (options.has("updateInterval")) {
                        UPDATE_INTERVAL = options.getLong("updateInterval");
                    }
                    if (options.has("fastestUpdateInterval")) {
                        FASTEST_UPDATE_INTERVAL = options.getLong("fastestUpdateInterval");
                    }
                    if (options.has("maxWaitTime")) {
                        MAX_WAIT_TIME = options.getLong("maxWaitTime") * UPDATE_INTERVAL;
                    }
                    if (options.has("token")) {
                        mApiToken = options.getString("token");
                    }
                    if (options.has("url")) {
                        mApiUrl = options.getString("url");
                    }
                    sendPluginResultAndKeepCallback("Ok",mCallbackContext);
                } catch (JSONException e) {
                    e.printStackTrace();
                    mCallbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void checkPermissions() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Boolean permissions = Utils.checkPermissions(mContext);
                sendPluginResultAndKeepCallback(permissions,mCallbackContext);
            }
        });
    }

    public void requestPermissions() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
               Utils.requestPermissions(mActivity);
            }
        });
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setMaxWaitTime(MAX_WAIT_TIME);
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(mContext, LocationBroadcastReceiver.class);
        intent.setAction(LocationBroadcastReceiver.ACTION_PROCESS_UPDATES);
        return PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == Utils.REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                Log.i(TAG, "User interaction was cancelled.");
                mCallbackContext.error("User interaction was cancelled.");
            } else if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
                    (grantResults[1] == PackageManager.PERMISSION_GRANTED)
            ) {
                start();
            } else {
                // Permission denied.
                mCallbackContext.error("Permission error");
            }
        }
    }

    /**
     * Handles the Request Updates button and requests start of location updates.
     */
    public void requestLocationUpdates() {
        try {
            Log.i(TAG, "Starting location updates");
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, getPendingIntent());
           sendPluginResultAndKeepCallback(true,mCallbackContext);
        } catch (SecurityException e) {
            e.printStackTrace();
            mCallbackContext.error(e.getMessage());
        }
    }

    /**
     * Handles the Remove Updates button, and requests removal of location updates.
     */
    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        mFusedLocationClient.removeLocationUpdates(getPendingIntent());
        sendPluginResultAndKeepCallback("OK",mCallbackContext);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    public static void sendData(JSONObject arg){
        if(mLocationCallbackContext !=null){
            sendPluginResultAndKeepCallback(arg,mLocationCallbackContext);
        }else {
            Log.i(TAG, "sendData: " +"no callbackcontext");
        }
    }

    protected static void sendPluginResultAndKeepCallback(String result, CallbackContext callbackContext) {
        PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, result);
        sendPluginResultAndKeepCallback(pluginresult, callbackContext);
    }

    protected static void sendPluginResultAndKeepCallback(JSONObject result, CallbackContext callbackContext) {
        PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, result);
        sendPluginResultAndKeepCallback(pluginresult, callbackContext);
    }

    protected static void sendPluginResultAndKeepCallback(Boolean result, CallbackContext callbackContext) {
        PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, result);
        callbackContext.sendPluginResult(pluginresult);
    }

    protected static void sendPluginResultAndKeepCallback(PluginResult pluginresult, CallbackContext callbackContext) {
        pluginresult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginresult);
    }
}
