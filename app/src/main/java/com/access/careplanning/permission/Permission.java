package com.access.careplanning.permission;

import android.Manifest;

/**
 * Permission enum - to wrap the android manifest strings for checking/requesting,
 * and used with its ordinal for callback ids.
 */
public enum Permission {

    CAMERA(Manifest.permission.CAMERA);

    final String manifestPermission;

    Permission(String permission) {
        manifestPermission = permission;
    }

    public static Permission getPermission(int id) {
        return id >= 0 && id < values().length ? values()[id] : null;
    }

}
