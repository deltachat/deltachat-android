package org.thoughtcrime.securesms.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

public class LocalNetworkPermission {

  private static final String TAG = "LocalNetworkPermission";

  private LocalNetworkPermission() {}

  public static boolean isNeeded() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN;
  }

  public static boolean hasPermission(Context context) {
    if (!isNeeded()) return true;
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK)
        == PackageManager.PERMISSION_GRANTED;
  }

  public static boolean isLocalAddress(String host) {
    if (host == null) return false;
    String hostBase = host.trim().toLowerCase(Locale.ROOT);
    if (hostBase.isEmpty()) return false;

    // remove ipv6 brackets
    if (hostBase.length() > 2 && hostBase.startsWith("[") && hostBase.endsWith("]")) {
      hostBase = hostBase.substring(1, hostBase.length() - 1);
    }

    if (hostBase.equals("localhost")) return false;
    if (hostBase.endsWith(".local")) return true;

    try {
      for (InetAddress address : InetAddress.getAllByName(hostBase)) {
        if (isLocalAddress(address)) return true;
      }
    } catch (UnknownHostException e) {
      Log.w(TAG, "cannot resolve " + host, e);
    }
    return false;
  }

  private static boolean isLocalAddress(InetAddress address) {
    if (address == null) return false;
    if (address.isLoopbackAddress()) return false;
    if (address.isLinkLocalAddress()) return true;
    if (address.isSiteLocalAddress()) return true;
    if (address instanceof Inet6Address) {
      byte[] bytes = address.getAddress();
      return (bytes[0] & 0xfe) == 0xfc;
    }
    return false;
  }
}
