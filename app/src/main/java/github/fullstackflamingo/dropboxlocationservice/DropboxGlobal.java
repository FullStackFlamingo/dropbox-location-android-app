package github.fullstackflamingo.dropboxlocationservice;

import android.content.Context;
import android.os.Build;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DropboxGlobal {
    public static DbxClientV2 locationDbxClient;
    // TODO: Replace "Build.MODEL" with device_name
    // https://developer.android.com/reference/android/provider/Settings.Global#DEVICE_NAME
    public static String thisDeviceFolder = Build.MODEL.replaceAll("\\W+", "");


    public static void setupClient(Context ctx) throws Exception {
//        AppPreferences.init(ctx);
//        final String ACCESS_TOKEN = AppPreferences.instance.getString("DbxAccessToken", null);
        String ACCESS_TOKEN = ctx.getString(R.string.DB_ACCESS_TOKEN);
        if (ACCESS_TOKEN == null) throw new Exception("No Dropbox Access Token");

        if (locationDbxClient == null) {
            DbxRequestConfig DbxLocationClientConfig = DbxRequestConfig.newBuilder("LocationUploadDBClient").build();
            locationDbxClient = new DbxClientV2(DbxLocationClientConfig, ACCESS_TOKEN);
        }
    }

    public static String getLocationFilename() {
        String dayFolder = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        return "/" + thisDeviceFolder + "/" + dayFolder + ".json";
    }
}
