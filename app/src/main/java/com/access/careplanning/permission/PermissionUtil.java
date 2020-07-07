package com.access.careplanning.permission;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PermissionUtil {

    private static final String TAG = "PermissionUtil";

    private static final int DIALOG_STYLE = android.R.style.Theme_Material_Light_Dialog_Alert;


    public enum GrantResult {GRANTED, DENIED, UNSPECIFIED}

    /**
     * Checks if the app has been granted a permission
     *
     * @param context
     * @param permission
     * @return true if permission is granted else false
     */
    public static boolean permitted(Context context, Permission permission) {
        boolean b = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || ContextCompat.checkSelfPermission(context, permission.manifestPermission) == PackageManager.PERMISSION_GRANTED;
        Log.i(TAG, "permitted " + permission.name() + " " + b);
        return b;
    }


    /**
     * Checks if a permission(s) has not been granted and if not, then requests it.
     * The callback to process the result must be done in the relevant activity (with ref the permission enum ordinal).
     *
     * @param activity
     * @param permissions one or more permissions to check
     * @return true if a permission is requested, else false when nothing required
     */
    public static boolean askPermission(Activity activity, Permission... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (Permission permission : permissions) {
                if (!permitted(activity, permission)) {
                    return askPermission(activity, permission.ordinal(), permissions);
                }
            }
        }
        return false;
    }

    /**
     * Checks if a permission(s) has not been granted and if not, then requests it.
     * The callback to process the result must be done in the relevant activity (with ref the callbackId).
     *
     * @param activity
     * @param callbackId  specific callback id eg so we can handle a particular case in a certain fragment of a permission request
     * @param permissions one or more permissions to check
     * @return true if a permission is requested, else false when nothing required
     */
    public static boolean askPermission(Activity activity, int callbackId, Permission... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            List<String> newPermissions = new ArrayList<>();
            for (Permission permission : permissions) {
                if (!permitted(activity, permission)) {
                    Log.i(TAG, "askPermission " + permission.name() + " by " + activity.getLocalClassName());
                    newPermissions.add(permission.manifestPermission);
                }
            }

            if (newPermissions.size() > 0) {
                ActivityCompat.requestPermissions(activity, newPermissions.toArray(new String[newPermissions.size()]), callbackId);
                return true;
            }
        }
        return false;
    }


    /**
     * Checks if a permission has just been granted
     *
     * @param permission
     * @param permissions
     * @param grantResults
     * @return true if now granted else false if not granted
     */
    public static boolean granted(Permission permission, String[] permissions, int[] grantResults) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            return true;
        }
        List<String> permissionsList = Arrays.asList(permissions);
        int pos = permissionsList.indexOf(permission.manifestPermission);
        Log.i(TAG, "granted now " + permission.name() + " " + (pos >= 0 && grantResults[pos] == 0));
        return pos >= 0 && grantResults[pos] == 0;
    }

    /**
     * Gets the state for a given permission
     *
     * @param permission
     * @param permissions
     * @param grantResults
     * @return Granted if was granted, Denied if was denied else Unspecified if not covered by the dialog
     */
    public static GrantResult grantedState(Permission permission, String[] permissions, int[] grantResults) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            return GrantResult.GRANTED;
        }
        List<String> permissionsList = Arrays.asList(permissions);
        int pos = permissionsList.indexOf(permission.manifestPermission);
        if (pos < 0) {
            return GrantResult.UNSPECIFIED;
        } else if (grantResults[pos] == 0) {
            return GrantResult.GRANTED;
        } else {
            return GrantResult.DENIED;
        }
    }

}
