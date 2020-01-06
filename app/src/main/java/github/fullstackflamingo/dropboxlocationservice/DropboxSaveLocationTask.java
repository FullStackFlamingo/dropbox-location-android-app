package github.fullstackflamingo.dropboxlocationservice;

import android.content.Context;
import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.UUID;

class DropboxSaveLocationTask extends AsyncTask<String, Void, FileMetadata> {
    private final String TAG = "DropboxSaveLocationTask";
    private Callback mCallback;
    private Exception mException;

    public DropboxSaveLocationTask(Context context, Callback callback) throws Exception {
        mCallback = callback;
        DropboxGlobal.setupClient(context);
    }

    public interface Callback {
        void onComplete(FileMetadata result);
        void onError(Exception e);
    }

    @Override
    protected void onPostExecute(FileMetadata result) {
        super.onPostExecute(result);
        if (mException != null) {
            mCallback.onError(mException);
        } else if (result == null) {
            mCallback.onError(null);
        } else {
            mCallback.onComplete(result);
        }
    }

    @Override
    protected FileMetadata doInBackground(String... params) {
        String remoteFileName = params[0];
        String data = params[1];
        FileMetadata existingFileMetaData = null;
        JSONArray existingFile = null;
        if (data != null) {
            // Download existing file.
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                existingFileMetaData = DropboxGlobal.locationDbxClient.files().download(remoteFileName)
                        .download(outputStream);
                String json = outputStream.toString("utf8");
                existingFile = new JSONArray(json);

            }catch (JSONException | DbxException | IOException e) {
                // no existing file
            }
            if (existingFile == null) {
                existingFile = new JSONArray();
            }

            JSONObject jsonDataObj = null;
            try {
                jsonDataObj = new JSONObject(data);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (jsonDataObj == null) {
                return null;
            }

            existingFile.put(jsonDataObj);
            String revision = existingFileMetaData != null ? existingFileMetaData.getRev() : UUID.randomUUID().toString().replace("-", "");
            try (InputStream inputStream = new ByteArrayInputStream(existingFile.toString().getBytes(StandardCharsets.UTF_8))) {
                return DropboxGlobal.locationDbxClient.files().uploadBuilder(remoteFileName)
                        .withMode(WriteMode.update(revision))
//                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(inputStream);
            } catch (DbxException | IOException e) {
                mException = e;
            }
        }

        return null;
    }
}