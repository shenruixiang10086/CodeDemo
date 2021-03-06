package com.example.srx.PermissionUtil;


/**
 * Created by ruixiang.shen on 2/25/17.
 */


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;


public class RequestPermissionsActivity extends Activity {
    private static final String TAG = "RequestPermissons";
    private static final String PREVIOUS_INTENT = "previous_intent";
    private static final int REQUEST_ALL_PERMISSIONS = 1;
    private Intent mPreviousIntent;

    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    public static boolean startPermissionActivity(Activity activity) {
        return startRequestPermissionActivity(activity, REQUIRED_PERMISSIONS,
                RequestPermissionsActivity.class);
    }

    public static boolean isPermissionsGranted(Context context) {
        return RequestPermissionsActivity.checkPermissions(
                context,
                REQUIRED_PERMISSIONS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreviousIntent = (Intent) getIntent().getExtras().get(
                PREVIOUS_INTENT);

        if (savedInstanceState == null) {
            requestPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantedResults) {
        if (permissions != null && permissions.length > 0
                && arePermissionsGranted(permissions, grantedResults)) {
            mPreviousIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(mPreviousIntent);
            finish();
        } else {
            Toast.makeText(this, "You have disabled a required permission.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    protected static boolean startRequestPermissionActivity(Activity activity,
                                                            String[] requiredPermissions, Class<?> newActivityClass) {
        if (!RequestPermissionsActivity.checkPermissions(activity,
                requiredPermissions)) {
            final Intent intent = new Intent(activity, newActivityClass);
            intent.putExtra(PREVIOUS_INTENT, activity.getIntent());
            activity.startActivity(intent);
            activity.finish();
            return true;
        }
        return false;
    }

    private static boolean checkPermissions(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean arePermissionsGranted(String permissions[], int[] grantResult) {
        for (int i = 0; i < permissions.length; i++) {
            if (grantResult[i] != PackageManager.PERMISSION_GRANTED
                    && Arrays.asList(REQUIRED_PERMISSIONS).contains(permissions[i])) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        final ArrayList<String> noGrantedPermissions = new ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (checkSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {
                noGrantedPermissions.add(permission);
            }
        }
        if (noGrantedPermissions.size() == 0) {

        }
        requestPermissions(noGrantedPermissions.toArray(new String[noGrantedPermissions.size()]),
                REQUEST_ALL_PERMISSIONS);
    }

}
