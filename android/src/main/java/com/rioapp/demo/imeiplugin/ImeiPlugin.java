package com.rioapp.demo.imeiplugin;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.UUID;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * ImeiPlugin
 */
public class ImeiPlugin implements MethodCallHandler, PluginRegistry.RequestPermissionsResultListener {

    private final Activity activity;
    private final ContentResolver contentResolver;

    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 1995;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID_99599";
    private Result mResult;
    private static boolean ssrpr = false;
    private static int slotSim = 0;

    /**
     * Plugin registration.
     * add Listener Request permission
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "imei_plugin");
        ImeiPlugin imeiPlugin = new ImeiPlugin(registrar.activity(), registrar.context().getContentResolver());
        channel.setMethodCallHandler(imeiPlugin);
        registrar.addRequestPermissionsResultListener(imeiPlugin);
    }

    private ImeiPlugin(Activity activity, ContentResolver contentResolver) {
        this.activity = activity;
        this.contentResolver = contentResolver;
    }

    private static void getImei(Activity activity, Result result) {
        
        try {
            if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.P){
                result.success(getUUID(activity));
            }else if (ContextCompat.checkSelfPermission((activity), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED){
                TelephonyManager telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    result.success( telephonyManager.getImei(slotSim) );
                } else {
                    result.success( telephonyManager.getDeviceId(slotSim) );

                }

            } else {

                if (ssrpr && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_PHONE_STATE) )
                    result.success("Permission Denied");
                else
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);

            }

        } catch (Exception ex) {
//            result.success("unknown");
        }
    }

    private synchronized static String getUUID(Context context) {

        SharedPreferences sharedPrefs = context.getSharedPreferences(
                PREF_UNIQUE_ID, Context.MODE_PRIVATE);
        String uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
        if (uniqueID == null) {
            uniqueID = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(PREF_UNIQUE_ID, uniqueID);
            editor.commit();
        }

        return uniqueID;
    }

    private static void getID(Context context, Result result){
        result.success(getUUID(context));
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        mResult = result;

        try {
            ssrpr = call.<Boolean>argument("ssrpr");
            slotSim = call.<Integer>argument("slotsim");
        } catch (Exception e){
            ssrpr = false;
            slotSim = 0;
        }

        if (call.method.equals("getImei"))
            getImei(activity, mResult);
        else if (call.method.equals("getId"))
            getID(activity, result);
        else
            mResult.notImplemented();

    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_PHONE_STATE) {
            if (results[0] == PackageManager.PERMISSION_GRANTED) {
                getImei(activity, mResult);
            } else {
                mResult.success("Permission Denied");
            }
            return true;
        }

        return false;
    }

}
