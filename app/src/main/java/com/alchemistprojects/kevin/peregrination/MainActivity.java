package com.alchemistprojects.kevin.peregrination;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MarkerOptions;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final int REQUEST_CHECK_SETTINGS = 1000;
    private static final int REQUEST_PERMISSIONS_LOCATION = 1001;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private LocationRequest mLocationRequest;
    private boolean mRequestedUpdates;
    private boolean mRequestedSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000)
                .setSmallestDisplacement(10);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                if (resultCode == RESULT_OK) {
                    checkLocationSettings();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSIONS_LOCATION:
                boolean canLocation = false;
                for (int grantResult : grantResults) {
                    if (grantResult == PERMISSION_GRANTED) {
                        canLocation = true;
                    }
                }
                if (canLocation) {
                    startLocationUpdates();
                }
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationSettings();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        mRequestedSettings = false;
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("MainActivity", "Google Api Client connected");
        checkLocationSettings();
    }

    private void checkLocationSettings() {
        if (mGoogleApiClient.isConnected() && !mRequestedSettings) {
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(mLocationRequest);
            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                            builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
//                final LocationSettingsStates = result.getLocationSettingsStates();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All location settings are satisfied. The client can
                            // initialize location requests here.
                            Log.d("checkLocationSettings", "All location settings satisfied.");
                            startLocationUpdates();
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied, but this can be fixed
                            // by showing the user a dialog.
                            Log.d("checkLocationSettings", "Location settings are not satisfied.");
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(
                                        MainActivity.this,
                                        REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way
                            // to fix the settings so we won't show the dialog.
                            Log.d("checkLocationSettings", "Location settings are not satisfied. However, we have no way to fix the settings so we won't show the dialog.");
                            break;
                    }
                }
            });
            mRequestedSettings = true;
        }
    }

    private void startLocationUpdates() {
        if (mGoogleApiClient.isConnected() && !mRequestedUpdates) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                String[] permissions =  {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION  
                };
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_LOCATION);
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mRequestedUpdates = true;
        }
    }

    private void stopLocationUpdates() {
        if (mRequestedUpdates) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mRequestedUpdates = false;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        StringBuilder sb = new StringBuilder();
        sb.append("Location Changed (");
        sb.append(location.getLongitude());
        sb.append(",");
        sb.append(location.getLatitude());
        sb.append(")");
        Log.d("Location Changed", sb.toString());
        Toast.makeText(this, sb.toString(), Toast.LENGTH_SHORT);
    }
}
