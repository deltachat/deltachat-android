package org.thoughtcrime.securesms.permissions;


import android.content.pm.PackageManager;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PermissionsRequest {

  private final Map<String, Boolean> PRE_REQUEST_MAPPING = new HashMap<>();

  private final @Nullable Runnable allGrantedListener;

  private final @Nullable Runnable anyDeniedListener;
  private final @Nullable Runnable anyPermanentlyDeniedListener;
  private final @Nullable Runnable anyResultListener;

  PermissionsRequest(@Nullable Runnable allGrantedListener,
                     @Nullable Runnable anyDeniedListener,
                     @Nullable Runnable anyPermanentlyDeniedListener,
                     @Nullable Runnable anyResultListener)
  {
    this.allGrantedListener            = allGrantedListener;

    this.anyDeniedListener             = anyDeniedListener;
    this.anyPermanentlyDeniedListener  = anyPermanentlyDeniedListener;
    this.anyResultListener             = anyResultListener;
  }

  void onResult(String[] permissions, int[] grantResults, boolean[] shouldShowRationaleDialog) {
    List<String> granted           = new ArrayList<>(permissions.length);
    List<String> denied            = new ArrayList<>(permissions.length);
    List<String> permanentlyDenied = new ArrayList<>(permissions.length);

    for (int i = 0; i < permissions.length; i++) {
      if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
        granted.add(permissions[i]);
      } else {
        boolean preRequestShouldShowRationaleDialog = PRE_REQUEST_MAPPING.get(permissions[i]);

        if (anyPermanentlyDeniedListener != null
            && !preRequestShouldShowRationaleDialog
            && !shouldShowRationaleDialog[i]) {
          permanentlyDenied.add(permissions[i]);
        } else {
          denied.add(permissions[i]);
        }
      }
    }

    if (allGrantedListener != null && !granted.isEmpty() && (denied.isEmpty() && permanentlyDenied.isEmpty())) {
      allGrantedListener.run();
    }

    if (!denied.isEmpty()) {
      if (anyDeniedListener != null)  anyDeniedListener.run();
    }

    if (!permanentlyDenied.isEmpty()) {
      if (anyPermanentlyDeniedListener != null)  anyPermanentlyDeniedListener.run();
    }

    if (anyResultListener != null) {
      anyResultListener.run();
    }
  }

  void addMapping(String permission, boolean shouldShowRationaleDialog) {
    PRE_REQUEST_MAPPING.put(permission, shouldShowRationaleDialog);
  }
}
