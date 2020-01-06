package github.fullstackflamingo.dropboxlocationservice;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

class MyLocationListener implements LocationListener {
    private Callback cb;
    public interface Callback {
        void onLocationChanged(Location loc);
    }
    public MyLocationListener(Callback cb) {
        this.cb=  cb;
    }
    @Override
    public void onLocationChanged(final Location loc) {
        cb.onLocationChanged(loc);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Log.e(TAG, "onStatusChanged");
    }

    @Override
    public void onProviderDisabled(String provider) {

        // Log.e(TAG, "onProviderDisabled");
    }

    @Override
    public void onProviderEnabled(String provider) {

        // Log.e(TAG, "onProviderEnabled");
    }
}