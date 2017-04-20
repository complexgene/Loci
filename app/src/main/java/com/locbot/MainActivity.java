package com.locbot;

import android.*;
import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private FusedLocationProviderApi locationProvider = LocationServices.FusedLocationApi;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    Button shareLocation, btn_shareLocation;
    TextView sharedFriend;
    public static double latVal, longVal, friendsLatiValue, friendsLongiValue;
    SingleTonUser singleTonUser;
    GoogleMap gMap;
    String friendsName, friendsNumber;

    private void init() {
        FirebaseApp.initializeApp(this);
        shareLocation = (Button) findViewById(R.id.shareLocation);
        btn_shareLocation = (Button) findViewById(R.id.shareLocation);
        /*latValue = (TextView) findViewById(R.id.latValue);
        longValue = (TextView) findViewById(R.id.longValue);*/
        singleTonUser = SingleTonUser.getInstance();
        sharedFriend = (TextView)findViewById(R.id.sharedFriend);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2 * 1000);
        locationRequest.setFastestInterval(1 * 1000);

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this);

        btn_shareLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(getApplicationContext(), ContactList.class);
                startActivityForResult(it, 1);
            }
        });

    }

    static boolean stopLocationSharing = false;
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Song dedicate //
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                String fromUser = singleTonUser.getMobileNumber();
                final String contactNameAndNumber = data.getStringExtra("selectedContact");
                String toUserNumber = contactNameAndNumber.split("\\n")[1];
                String toUserName = contactNameAndNumber.split("\\n")[0];
                Log.e("assdd", toUserName+" "+toUserNumber);

                friendsName = toUserName;
                friendsNumber = toUserNumber;

                SwitchCompat sc = (SwitchCompat) findViewById(R.id.sharingStatus);
                sc.setChecked(true);
                sc.setEnabled(true);
                sharedFriend.setText(friendsName);

                sc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if(!isChecked) {
                                Toast.makeText(getApplicationContext(), "!!_You stopped location sharing_!!", Toast.LENGTH_SHORT).show();
                                stopLocationSharing = true;
                            }
                    }
                });
                liveFriendLocationDetection(toUserNumber);
                friendAlive = true;
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                )
        {
            Log.e("Start_Permission", "Asking Permission as not yet asked...");
            int fineLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            int coarseLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            int contactsWPermssion = checkSelfPermission(Manifest.permission.WRITE_CONTACTS);
            int contactsRPermssion = checkSelfPermission(Manifest.permission.READ_CONTACTS);
            if (fineLocationPermission != PackageManager.PERMISSION_GRANTED &&
                    coarseLocationPermission != PackageManager.PERMISSION_GRANTED &&
                    contactsWPermssion != PackageManager.PERMISSION_GRANTED &&
                    contactsRPermssion != PackageManager.PERMISSION_GRANTED
                    ) { // Ask for the permissions if any of the permissions not given yet..
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.READ_CONTACTS,
                }, 101);
            }
        }
        else {
            Log.e("yayyy", "Permission once given..");
            HashMap<String, String> allContacts = ContactList.getContactNames(this.getContentResolver());
            AllAppData.setAllContacts(allContacts);
            Log.e("allCCC", allContacts.toString());
            // Contacts Reading done, set button VISIBLE...
            btn_shareLocation.setVisibility(View.VISIBLE);
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            gMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 101: {
                Log.e("yayyy", "Permission granted yayyy");
                requestLocationUpdates();
            }
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
        Log.e("Heyyy", "Got called");
        latVal = location.getLatitude();
        longVal = location.getLongitude();

        if(!stopLocationSharing)
            writeTheChangeOnCloudDB(latVal, longVal);
        else
            resetSharingValuesOnCloud(singleTonUser.getMobileNumber(), friendsNumber);

        this.onMapReady(gMap);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (googleApiClient.isConnected()) {
            requestLocationUpdates();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    private MarkerOptions friendsMarker;
    private LatLng friendCoordinates;
    static int i = 0;
    private boolean friendAlive = false;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        googleMap.clear();

        if(friendAlive) {
            friendCoordinates = new LatLng(latVal + 0.0021, longVal + 0.00021);
            friendsMarker = new MarkerOptions().position(friendCoordinates).title(friendsName).visible(true);

            friendsMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_friend));
            friendsMarker.position(friendCoordinates);
            googleMap.addMarker(friendsMarker).showInfoWindow();

            if (latVal != 0 || longVal != 0)
                getDirections(new LatLng(latVal, longVal), friendCoordinates);
            if (i++ <= 1)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(friendCoordinates, 16.0f));
        }
        else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latVal, longVal), 16.0f));
        }
    }

    private void resetSharingValuesOnCloud(String myMobileNumber, String friendsMobileNumber){
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference meSharingToFriend = firebaseDatabase.getReference().child("requests").child(myMobileNumber).child(friendsMobileNumber);
        DatabaseReference friendSharingToMe = firebaseDatabase.getReference().child("requests").child(friendsMobileNumber).child(myMobileNumber);
        meSharingToFriend.setValue(0);
        friendSharingToMe.setValue(0);
    }

    public void monitorLocationShareRequests() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference meSharingToFriend = firebaseDatabase.getReference().child("requests").child(singleTonUser.getMobileNumber()).child("from");
        meSharingToFriend.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int value = dataSnapshot.getValue(Integer.class);
                if(value != -1) {
                    // Check if app is foregroudn or background and generate alert based on that
                    // if(approved)
                    // Set the value as the mobile number of friend.
                    //else
                    //  meSharingToFriend.setValue(-1);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void liveFriendLocationDetection(String friendMobileNumber) {
        Log.e("FRNDMOBNO", friendMobileNumber);
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference liveLocLatiRef = firebaseDatabase.getReference().child("users").child(friendMobileNumber).child("latiValue");
        DatabaseReference liveLocLongiRef = firebaseDatabase.getReference().child("users").child(friendMobileNumber).child("longiValue");

        liveLocLatiRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                double latiValue = dataSnapshot.getValue(Double.class);
                Log.e("MA_lati", ""+latiValue);
                onMapReady(gMap);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        liveLocLongiRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                double longiValue = dataSnapshot.getValue(Double.class);
                Log.e("MA_longi", ""+longiValue);
                onMapReady(gMap);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void writeTheChangeOnCloudDB(double latiVal, double longiVal) {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference liveLocLatiRef = firebaseDatabase.getReference().child("users").child(singleTonUser.getMobileNumber()).child("latiValue");
        DatabaseReference liveLocLongiRef = firebaseDatabase.getReference().child("users").child(singleTonUser.getMobileNumber()).child("longiValue");

        liveLocLatiRef.setValue(latiVal);
        liveLocLongiRef.setValue(longiVal);
    }

    public void getDirections(LatLng origin, LatLng dest) {
        String directionUrl = getDirectionsUrl(origin, dest);
        Log.e("DIRECTIONS", directionUrl);
        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(directionUrl);
    }
    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=driving";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        return url;
    }
    private class DownloadTask extends AsyncTask {

        @Override
        protected String doInBackground(Object[] params) {

            String data = "";

            try {
                Log.e("HERE", "here1");
                data = downloadUrl(String.valueOf(params[0]));
                Log.e("HERE", "here1:"+data);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            Log.e("HERE", "here2");
            ParserTask parserTask = new ParserTask();
            Log.e("HERE", "here2:"+String.valueOf(o));
            parserTask.execute(String.valueOf(o));
        }

        private String downloadUrl(String strUrl) throws IOException {
            String data = "";
            InputStream iStream = null;
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(strUrl);

                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.connect();

                iStream = urlConnection.getInputStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

                StringBuffer sb = new StringBuffer();

                String line = "";
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                data = sb.toString();

                br.close();

            } catch (Exception e) {
                Log.d("Exception", e.toString());
            } finally {
                iStream.close();
                urlConnection.disconnect();
            }
            return data;
        }

    }
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... params) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(String.valueOf(params[0]));
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            Log.e("RESULT", result.toString());

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat").toString());
                    double lng = Double.parseDouble(point.get("lng").toString());
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(14);
                lineOptions.color(Color.BLACK);
                lineOptions.geodesic(true);

            }
            Log.e("LINE", lineOptions.toString());
// Drawing polyline in the Google Map for the i-th route
            gMap.addPolyline(lineOptions);
        }
    }
}