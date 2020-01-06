package github.fullstackflamingo.dropboxlocationservice;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

// TODO: Use encrypted preferences for dropbox access token rather than building app with secrets.xml
public class AppPreferences {
    public static SharedPreferences instance = null;
    public static SharedPreferences.Editor instanceEditor = null;

    public static void init(Context ctx) throws GeneralSecurityException, IOException {
        if (instance != null) return;

        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        instance = EncryptedSharedPreferences.create(
                "github.fullstackflamingo.dropboxlocationservice:cryptosharedprefs",
                masterKeyAlias,
                ctx,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        instanceEditor = instance.edit();
    }
}
