package github.fullstackflamingo.dropboxlocationservice;


import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.dropbox.core.v2.files.FileMetadata;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LocationUploadWorker extends Worker {

    private static final String TAG = "LocationUploadWorker";
    private static final String WORKER_TAG = "github.fullstackflamingo.dropboxlocationservice.LocationUploadWorker";
    private static HandlerThread mHandlerThread;
    private static CountDownLatch locationWait;
    private static CountDownLatch dropboxWait;
    private final static Long MAX_WAIT_TIME_LOCATION_MS = 5 * 60 * 1000L;


    private static String getLocationDataJSONString(String provider, Double lon, Double lat, Long locationAcquisitionTime) {
        String template = "{ \"lon\": %s, \"lat\": %s, \"date_ms\": %s, \"provider\": %s, \"location_acquisition_time_ms\": %s}";
        return String.format(template, lon, lat, System.currentTimeMillis(), provider, locationAcquisitionTime);
    }

    public LocationUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void enqueueSelf(@NonNull Context context) {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType
                .CONNECTED).build();
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                LocationUploadWorker.class, 15, TimeUnit.MINUTES, 5, TimeUnit.MINUTES
        ).setConstraints(constraints).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORKER_TAG, ExistingPeriodicWorkPolicy.KEEP, work);
    }

    @SuppressLint({"MissingPermission", "WrongThread"})
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Dropbox Location work started");
        Context context = getApplicationContext();
        mHandlerThread = new HandlerThread("LocationDropboxThread");
        mHandlerThread.start();
        LocationManager locationManager = null;
        MyLocationListener locListener = null;
        Handler locationTimeoutHandler = new Handler(mHandlerThread.getLooper());
        Runnable locationTimeoutRunnable = null;
        try {
            final String[] data = {null};

//          GET LOCATION
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            final String bestProvider = locationManager.getBestProvider(new Criteria(), true);
            Log.d(TAG, "Dropbox Location request bestProvider: " + bestProvider);
            if (bestProvider == null) {
                throw new Exception("can't get location provider");
            }
//          GET LOCATION: REQUEST LON/LAT AND RECORD IT
            final Long locationStartTime = System.currentTimeMillis();
            Log.d(TAG, "Dropbox Location request started: " + locationStartTime);
            locListener = new MyLocationListener(new MyLocationListener.Callback() {
                @Override
                public void onLocationChanged(Location loc) {
                    Long locationAcquisitionTime = (System.currentTimeMillis() - locationStartTime );
                    Log.d(TAG, "Dropbox Location request finished - total time: " + locationAcquisitionTime / 1000 + "s");
                    data[0] = getLocationDataJSONString(bestProvider, loc.getLongitude(), loc.getLatitude(), locationAcquisitionTime);
                    locationWait.countDown();
                }
            });
            locationManager.requestSingleUpdate(bestProvider, locListener, mHandlerThread.getLooper());
//          GET LOCATION: TIMEOUT FOR LOCATION REQUEST
            final LocationManager finalLocationManager = locationManager;
            final MyLocationListener finalLocListener = locListener;
            locationTimeoutRunnable = new Runnable() {
                public void run() {
                    // STOP AWAITING FOR LOCATION IF TIMEOUT
                    locationWait.countDown();
                    finalLocationManager.removeUpdates(finalLocListener);

                }
            };
            locationTimeoutHandler.postDelayed(locationTimeoutRunnable, MAX_WAIT_TIME_LOCATION_MS);
            locationWait = new CountDownLatch(1);
            locationWait.await();
            locationManager.removeUpdates(locListener);

            if (data[0] == null) {
                throw new Exception("can't get location");
            }
//          END GET LOCATION

//          UPLOAD TO DROPBOX
            Log.d(TAG, "Dropbox Location upload started: " + System.currentTimeMillis());
            String filename = DropboxGlobal.getLocationFilename();
            new DropboxSaveLocationTask(context, new DropboxSaveLocationTask.Callback() {
                @Override
                public void onComplete(FileMetadata result) {
                    LocationUploadWorker.dropboxWait.countDown();
                }

                @Override
                public void onError(Exception e) {
                    LocationUploadWorker.dropboxWait.countDown();

                }
            }).execute(filename, data[0]);
            dropboxWait = new CountDownLatch(1);
            dropboxWait.await();
            Log.d(TAG, "Dropbox Location upload finished: " + System.currentTimeMillis());
//          END UPLOAD TO DROPBOX

            return Result.success();
        } catch (Exception e) {
            Log.d(TAG, "Dropbox Location failed: " + System.currentTimeMillis());
            e.printStackTrace();
            return Result.failure();
        }finally {
            mHandlerThread.getLooper().quit();
            mHandlerThread.quit();
            if (locationTimeoutRunnable != null) {
                locationTimeoutHandler.removeCallbacks(locationTimeoutRunnable);
            }
            if (locationManager != null && locListener != null) {
                locationManager.removeUpdates(locListener);
            }
        }
    }
}
