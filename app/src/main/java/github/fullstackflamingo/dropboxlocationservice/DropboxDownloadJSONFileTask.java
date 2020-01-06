package github.fullstackflamingo.dropboxlocationservice;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;


class DropboxDownloadJSONFileTask extends AsyncTask<FileMetadata, Void, JSONArray> {
    private final String TAG = "DropboxDownloadJSONFileTask";
    private final Callback mCallback;
    private Exception mException;

    public interface Callback {
        void onComplete(JSONArray result);
        void onError(Exception e);
    }

    DropboxDownloadJSONFileTask(Context ctx, Callback callback) throws Exception {
        mCallback = callback;
        DropboxGlobal.setupClient(ctx);
    }

    @Override
    protected void onPostExecute(JSONArray result) {
        super.onPostExecute(result);
        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onComplete(result);
        }
    }

    @Override
    protected JSONArray doInBackground(FileMetadata... params) {
        FileMetadata metadata = params[0];
        try {
            File path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
//            File file = new File(path, metadata.getName());

            // Make sure the Downloads directory exists.
            if (!path.exists()) {
                if (!path.mkdirs()) {
                    mException = new RuntimeException("Unable to create directory: " + path);
                }
            } else if (!path.isDirectory()) {
                mException = new IllegalStateException("Download path is not a directory: " + path);
                return null;
            }

            // Download the file.
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                DropboxGlobal.locationDbxClient.files().download(metadata.getPathLower(), metadata.getRev())
                        .download(outputStream);
            }catch (DbxException | IOException e) {
                throw e;
            }

            // Tell android about the file
            /*Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            mContext.sendBroadcast(intent);*/
            String json = outputStream.toString("utf8");
            try {
                return new JSONArray(json);
            } catch (JSONException e) {
                throw e;
            }
        } catch (DbxException | IOException | JSONException e) {
            mException = e;
        }

        return null;
    }
}
