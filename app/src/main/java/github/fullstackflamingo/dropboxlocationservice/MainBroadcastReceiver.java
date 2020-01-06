package github.fullstackflamingo.dropboxlocationservice;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MainBroadcastReceiver extends BroadcastReceiver {
        public void onReceive( Context context, Intent intent ) {
            if( intent.getAction() == null ) return;
//            Log.e("MainBroadcastReceiver", intent.getAction());
            LocationUploadWorker.enqueueSelf(context);
        }
    }
