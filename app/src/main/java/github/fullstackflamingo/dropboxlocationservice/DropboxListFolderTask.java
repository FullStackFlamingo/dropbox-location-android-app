package github.fullstackflamingo.dropboxlocationservice;


import android.content.Context;
import android.os.AsyncTask;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.ListFolderResult;

import java.io.IOException;
import java.security.GeneralSecurityException;

class DropboxListFolderTask extends AsyncTask<String, Void, ListFolderResult> {
    private final String TAG = "DropboxListFolderTask";
    private final Callback mCallback;
    private Exception mException;

    public DropboxListFolderTask(Context ctx, Callback callback) throws Exception {
        mCallback = callback;
        DropboxGlobal.setupClient(ctx);
    }

    public interface Callback {
        void onComplete(ListFolderResult result);

        void onError(Exception e);
    }

    @Override
    protected void onPostExecute(ListFolderResult result) {
        super.onPostExecute(result);

        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onComplete(result);
        }
    }

    @Override
    protected ListFolderResult doInBackground(String... params) {
        try {
            return DropboxGlobal.locationDbxClient.files().listFolder(params[0]);
        } catch (DbxException e) {
            mException = e;
        }

        return null;
    }
}