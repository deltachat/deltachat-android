package org.thoughtcrime.securesms.crypto;


import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.util.Prefs;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * It can be rather expensive to read from the keystore, so this class caches the key in memory
 * after it is created.
 */
public final class DatabaseSecretProvider {

  private static final ConcurrentHashMap<Integer, DatabaseSecret> instances = new ConcurrentHashMap<>();

  public static DatabaseSecret getOrCreateDatabaseSecret(@NonNull Context context, int accountId) {
    if (instances.get(accountId) == null) {
      synchronized (DatabaseSecretProvider.class) {
        if (instances.get(accountId) == null) {
          instances.put(accountId, getOrCreate(context, accountId));
        }
      }
    }

    return instances.get(accountId);
  }

  private DatabaseSecretProvider() {
  }

  private static @NonNull DatabaseSecret getOrCreate(@NonNull Context context, int accountId) {
    String unencryptedSecret = Prefs.getDatabaseUnencryptedSecret(context, accountId);
    String encryptedSecret   = Prefs.getDatabaseEncryptedSecret(context, accountId);

    if      (unencryptedSecret != null) return getUnencryptedDatabaseSecret(context, unencryptedSecret, accountId);
    else if (encryptedSecret != null)   return getEncryptedDatabaseSecret(encryptedSecret);
    else                                return createAndStoreDatabaseSecret(context, accountId);
  }

  private static @NonNull DatabaseSecret getUnencryptedDatabaseSecret(@NonNull Context context, @NonNull String unencryptedSecret, int accountId)
  {
    try {
      DatabaseSecret databaseSecret = new DatabaseSecret(unencryptedSecret);

      if (Build.VERSION.SDK_INT < 23) {
        return databaseSecret;
      } else {
        KeyStoreHelper.SealedData encryptedSecret = KeyStoreHelper.seal(databaseSecret.asBytes());

        Prefs.setDatabaseEncryptedSecret(context, encryptedSecret.serialize(), accountId);
        Prefs.setDatabaseUnencryptedSecret(context, null, accountId);

        return databaseSecret;
      }
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private static @NonNull DatabaseSecret getEncryptedDatabaseSecret(@NonNull String serializedEncryptedSecret) {
    if (Build.VERSION.SDK_INT < 23) {
      throw new AssertionError("OS downgrade not supported. KeyStore sealed data exists on platform < M!");
    } else {
      KeyStoreHelper.SealedData encryptedSecret = KeyStoreHelper.SealedData.fromString(serializedEncryptedSecret);
      return new DatabaseSecret(KeyStoreHelper.unseal(encryptedSecret));
    }
  }

  private static @NonNull DatabaseSecret createAndStoreDatabaseSecret(@NonNull Context context, int accountId) {
    SecureRandom random = new SecureRandom();
    byte[]       secret = new byte[32];
    random.nextBytes(secret);

    DatabaseSecret databaseSecret = new DatabaseSecret(secret);

    if (Build.VERSION.SDK_INT >= 23) {
      KeyStoreHelper.SealedData encryptedSecret = KeyStoreHelper.seal(databaseSecret.asBytes());
      Prefs.setDatabaseEncryptedSecret(context, encryptedSecret.serialize(), accountId);
    } else {
      Prefs.setDatabaseUnencryptedSecret(context, databaseSecret.asString(), accountId);
    }

    return databaseSecret;
  }
}
