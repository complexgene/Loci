package com.locbot;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
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
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private FusedLocationProviderApi locationProvider = LocationServices.FusedLocationApi;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    Button shareLocation, btn_shareLocation;
    TextView sharedFriend;
    public static double latVal, longVal;
    SingleTonUser singleTonUser;
    GoogleMap gMap;
    String friendsName, friendsNumber;

    private void init() {
        FirebaseApp.initializeApp(this);
        sc = (SwitchCompat) findViewById(R.id.sharingStatus);
        shareLocation = (Button) findViewById(R.id.shareLocation);
        btn_shareLocation = (Button) findViewById(R.id.shareLocation);
        /*latValue = (TextView) findViewById(R.id.latValue);
        longValue = (TextView) findViewById(R.id.longValue);*/
        singleTonUser = SingleTonUser.getInstance();
        sharedFriend = (TextView)findViewById(R.id.sharingWith);
    }

    public boolean populateUserData(){
        StoreRetrieveDataInterface rd = null;
        try {
            Log.e("UserManagementService","Reading Userdata.txt file from local and populating the POJO");
            rd = new StoreRetrieveDataImpl("UserData.txt");
            if (rd.fileExists()) {
                rd.beginReadTransaction();
                singleTonUser.setName(rd.getValueFor(AllAppData.userName));
                singleTonUser.setMobileNumber(rd.getValueFor(AllAppData.userMobileNumber));
                rd.endReadTransaction();
                Log.e("UserManagementService","User object populated:\n" + singleTonUser.toString());
                return true;
            }
            else {

                return false;
            }
        }
        catch(Exception ee){
            Log.e("UserManagementService","Reading file from local encountered some problem..");
            ee.fillInStackTrace();
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        if(populateUserData()) {

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

            monitorLocationShareRequests();
        }
        else {
            Log.e("Start_USER_STATUS","User opening app first time..");
            Intent ii = new Intent(this, Registration.class);
            ii.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ii.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // clears all previous activities task
            finish(); // destroy current activity..
            startActivity(ii);
        }
    }
    SwitchCompat sc;
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

                FirebaseDatabase fd = FirebaseDatabase.getInstance();
                final DatabaseReference drMine = fd.getReference().child("requests").child(singleTonUser.getMobileNumber()).child("from");
                final DatabaseReference drFrined = fd.getReference().child("requests").child(friendsNumber).child("from");

                drFrined.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String value = dataSnapshot.getValue(String.class);
                        if(!value.equals("-1")) {
                            Toast.makeText(getApplicationContext(),"!" + AllAppData.getAllContacts().get(friendsNumber) + " sharing location with someone!", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            //drFrined.setValue(singleTonUser.getMobileNumber());
                            drMine.setValue(friendsNumber);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
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
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                )
        {
            Log.e("Start_Permission", "Asking Permission as not yet asked...");
            int fineLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            int coarseLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            int extStoragePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int contactsWPermssion = checkSelfPermission(Manifest.permission.WRITE_CONTACTS);
            int contactsRPermssion = checkSelfPermission(Manifest.permission.READ_CONTACTS);
            if (fineLocationPermission != PackageManager.PERMISSION_GRANTED &&
                    coarseLocationPermission != PackageManager.PERMISSION_GRANTED &&
                    contactsWPermssion != PackageManager.PERMISSION_GRANTED &&
                    contactsRPermssion != PackageManager.PERMISSION_GRANTED &&
                    extStoragePermission != PackageManager.PERMISSION_GRANTED
                    ) { // Ask for the permissions if any of the permissions not given yet..
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
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
            //monitorLocationShareRequests();
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

        writeTheChangeOnCloudDB(latVal, longVal);
        //monitorLocationShareRequests();
        this.onMapReady(gMap);
    }

    private MarkerOptions friendsMarker;
    private LatLng friendCoordinates;
    static int i = 0;
    private boolean friendAlive = false;

    private double friendsLatiValue, friendsLongiValue;
    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        googleMap.clear();

        if(friendAlive) {
            Log.e("FriendAliveNow", "Permitted " + friendsLatiValue + " " + friendsLongiValue);
            friendCoordinates = new LatLng(friendsLatiValue, friendsLongiValue);
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

    public void monitorLocationShareRequests() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference meSharingToFriend = firebaseDatabase.getReference("requests").child(singleTonUser.getMobileNumber());
        meSharingToFriend.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                meSharingToFriend.child("from").setValue("-1");
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.e("Value", "Changed triggered...");
                String value = dataSnapshot.getValue(String.class);
                Log.e("Vals",value);
                if(!value.equals("-1")) {
                    Log.e("Value", "Changed");
                    friendsNumber = value;
                    friendsName = AllAppData.getAllContacts().get(value);
                    Log.e("Value", value+" "+friendsName);
                    displayAlertNotificationOnTopBarOfPhone(getApplicationContext());
                }
                else {
                    liveFriendLocationDetection("-1");
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void displayAlertNotificationOnTopBarOfPhone(final Context context){
        // Getting number of last unseen notifications from file and add the current unseen to get total unseen
        //final Activity currActivity = (Activity)context;
        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.icon_friend)
                        .setColor(001500)
                        .setContentTitle("LocBot")
                        .setWhen(System.currentTimeMillis())
                        .setTicker(singleTonUser.getName())
                        .setContentText(friendsName +" has sent you a location sync request. !!");

        final Intent notificationIntent = new Intent(context, MainActivity.class);

        if(context==null) {
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        else {
                    if(isAppForground(context)) {
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which){
                                    case DialogInterface.BUTTON_POSITIVE:
                                        Toast.makeText(getApplicationContext(),"Location In Sync", Toast.LENGTH_SHORT).show();
                                        btn_shareLocation.setVisibility(View.GONE);
                                        liveFriendLocationDetection(friendsNumber);
                                        sharedFriend.setText("Sharing with : " + friendsName);
                                        sc.setChecked(true);
                                        sc.setEnabled(true);
                                        sc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                            @Override
                                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                if(!isChecked) {
                                                    Toast.makeText(getApplicationContext(), "!!_Sharing Stopped_!!", Toast.LENGTH_SHORT).show();
                                                    btn_shareLocation.setVisibility(View.VISIBLE);
                                                    sharedFriend.setText("! Not Sharing !");
                                                    friendAlive = false;
                                                    sc.setEnabled(false);
                                                    FirebaseDatabase fd = FirebaseDatabase.getInstance();
                                                    DatabaseReference drMine = fd.getReference().child("requests").child(singleTonUser.getMobileNumber()).child("from");
                                                    DatabaseReference drFrined = fd.getReference().child("requests").child(friendsNumber).child("from");
                                                    drMine.setValue("-1");
                                                    drFrined.setValue("-1");
                                                }
                                            }
                                        });
                                        break;
                                    case DialogInterface.BUTTON_NEGATIVE:
                                        FirebaseDatabase fd = FirebaseDatabase.getInstance();
                                        DatabaseReference drMine = fd.getReference().child("requests").child(singleTonUser.getMobileNumber()).child("from");
                                        DatabaseReference drFrined = fd.getReference().child("requests").child(friendsNumber).child("from");
                                        drMine.setValue("-1");
                                        drFrined.setValue("-1");
                                        break;
                                }
                            }
                        };
                        AlertDialog.Builder builder2 = new AlertDialog.Builder(MainActivity.this);
                        builder2.setTitle("!! Location Sync request from " + friendsName +" !!");
                        Dialog d=builder2.setMessage("This will allow " + friendsName + " to view your live location.").setPositiveButton("Allow", dialogClickListener)
                                .setNegativeButton("Cancel", dialogClickListener).show();

                    }
                    else{
                        Log.e("ServerManager","App is running in background..");
                        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
                        builder.setContentIntent(contentIntent);
                        builder.setAutoCancel(true);

                        // Add as notification
                        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        manager.notify(0, builder.build());
                    }
                }
        //-------------Play Notification sound-------------------------------------------------------------
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(context, notification);
        r.play();
    }

    public boolean isAppForground(Context mContext) {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(mContext.getPackageName())) {
                return false;
            }
        }
        return true;
    }

    public void liveFriendLocationDetection(String friendMobileNumber) {
        Log.e("FRNDMOBNO", friendMobileNumber);
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference liveLocLatiRef = firebaseDatabase.getReference().child("users").child(friendMobileNumber).child("latiValue");
        DatabaseReference liveLocLongiRef = firebaseDatabase.getReference().child("users").child(friendMobileNumber).child("longiValue");


        ValueEventListener v = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                double latiValue = dataSnapshot.getValue(Double.class);
                Log.e("MA_lati", ""+latiValue);
                friendsLatiValue = latiValue;
                if(friendsLongiValue > 0)
                    friendAlive = true;
                onMapReady(gMap);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        ValueEventListener v1 = new ValueEventListener() {
                @Override
                public void onDataChange (DataSnapshot dataSnapshot){
                    double longiValue = dataSnapshot.getValue(Double.class);
                    Log.e("MA_longi", "" + longiValue);
                    friendsLongiValue = longiValue;
                    if (friendsLatiValue > 0)
                        friendAlive = true;
                    onMapReady(gMap);
                }

                @Override
                public void onCancelled (DatabaseError databaseError){

                }
            };

        if(friendMobileNumber.equals("-1")) {
            liveLocLatiRef.removeEventListener(v);
            liveLocLongiRef.removeEventListener(v1);
            friendAlive = false;
        }
        else {
            liveLocLatiRef.addValueEventListener(v);
            liveLocLongiRef.addValueEventListener(v1);
        }

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

}