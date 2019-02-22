package fr.innodev.trd.gpsbasedemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Location myLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Log log;
    private int nbPos;
    private boolean first;
    private Marker latestPosMark;
    private LatLng curPos;
    private LatLng lastPos;
    private LatLng firstPos;
    private Marker firstPosMark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        nbPos = 0;
        first = true;
        latestPosMark = null;
        curPos = null;
        lastPos = null;
        firstPos = null;

        while (!permissionGranted()) ;

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            log.v("INFO", "Location Result" + location.toString());
                            updateMapDisplay(location);
                        }
                    }
                });

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    log.v("INFO", "Location Callback" + location.toString());
                    updateMapDisplay(location);
                }
            }
        };

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

    }

    private void updateMapDisplay(Location myLocation) {
        // Add a marker in Sydney and move the camera
        if(latestPosMark!=null) {
            lastPos = curPos;
        }
        curPos = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
        if(first){
            firstPos = curPos;
            float zoom = mMap.getMaxZoomLevel();
            log.d("INFO", "Zoom Max = " + zoom);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curPos, zoom - 3.0f));
            first = false;
            firstPosMark = mMap.addMarker(new MarkerOptions()
                    .position(firstPos)
                    .title("Position  0")
                    .visible(false)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            latestPosMark = mMap.addMarker(new MarkerOptions()
                    .position(firstPos)
                    .title("Position  " + nbPos));
        }else {
            firstPosMark.setVisible(true);
            latestPosMark.setVisible(false);
            latestPosMark.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            mMap.addPolyline(new PolylineOptions()
                    .add(lastPos, curPos)
                    .width(5)
                    .color(Color.RED));
            Log.d("INFO", "updateMapDisplay: "+getDistanceFromLatLonInKm(lastPos.latitude,lastPos.longitude,curPos.latitude,curPos.longitude)*1000+"m");
            latestPosMark = mMap.addMarker(new MarkerOptions()
                    .position(curPos)
                    .title("Position " + nbPos));
        }
        nbPos++;

    }


    private double getDistanceFromLatLonInKm(double lat1, double lon1,double lat2,double lon2) {
        int R = 6371; // Radius of the earth in km
        double dLat = deg2rad(lat2-lat1);  // deg2rad below
        double dLon = deg2rad(lon2-lon1);
        double a =
                Math.sin(dLat/2) * Math.sin(dLat/2) +
                        Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                                Math.sin(dLon/2) * Math.sin(dLon/2)
                ;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c; // Distance in km
        return d;
    }

    private double deg2rad(double deg) {
        return deg * (Math.PI/180);
    }

    private boolean permissionGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {//Can add more as per requirement

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    123);
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
