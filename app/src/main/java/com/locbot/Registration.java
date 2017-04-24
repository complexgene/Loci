package com.locbot;

import android.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;

public class Registration extends AppCompatActivity {

    private SingleTonUser singleTonUserObject;
    private StoreRetrieveDataInterface rd;
    EditText name, phno;
    Button register;

    private void init() {
        FirebaseApp.initializeApp(this);
        singleTonUserObject = SingleTonUser.getInstance();
        name = (EditText)findViewById(R.id.name);
        phno = (EditText) findViewById(R.id.mobileNo);
        register = (Button)findViewById(R.id.register);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        requestLocationUpdates();

        init();

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                singleTonUserObject.setName(name.getText().toString());
                singleTonUserObject.setMobileNumber(phno.getText().toString());

                if(createUserProfileFileinLocal(singleTonUserObject)) {
                    Intent ii = new Intent(Registration.this, MainActivity.class);
                    ii.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    ii.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // clears all previous activities task
                    finish(); // destroy current activity..
                    startActivity(ii);
                }
            }
        });

    }

    public boolean createUserProfileFileinLocal(SingleTonUser singleTonUser) {
        try {
            // Storing all the registration details into the text file
            Log.e("UserManagementService","Creating UserData.txt file and writing user data into that..");
            rd = new StoreRetrieveDataImpl("UserData.txt");
            rd.beginWriteTransaction();
            rd.createNewData(AllAppData.userName, singleTonUser.getName());
            rd.createNewData(AllAppData.userMobileNumber, singleTonUser.getMobileNumber());
            rd.endWriteTransaction();
            Log.e("UserManagementService","Creating text file in local and writing user data to that completed..");
            FirebaseDatabase fd = FirebaseDatabase.getInstance();
            final DatabaseReference drMine = fd.getReference().child("requests").child(singleTonUser.getMobileNumber()).child("from");
            drMine.setValue("-1");
            return true;
        } catch (IOException e) {
            Log.e("UserManagementSer_Err", "Couldn't save the local file..");
            return false;
        }
    }

    private void requestLocationUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                )
        {
            Log.e("Start_Permission", "Asking Permission as not yet asked...");
            int fineLocationPermission = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
            int coarseLocationPermission = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
            int extStoragePermission = checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int contactsWPermssion = checkSelfPermission(android.Manifest.permission.WRITE_CONTACTS);
            int contactsRPermssion = checkSelfPermission(android.Manifest.permission.READ_CONTACTS);
            if (fineLocationPermission != PackageManager.PERMISSION_GRANTED &&
                    coarseLocationPermission != PackageManager.PERMISSION_GRANTED &&
                    contactsWPermssion != PackageManager.PERMISSION_GRANTED &&
                    contactsRPermssion != PackageManager.PERMISSION_GRANTED &&
                    extStoragePermission != PackageManager.PERMISSION_GRANTED
                    ) { // Ask for the permissions if any of the permissions not given yet..
                requestPermissions(new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.WRITE_CONTACTS,
                        android.Manifest.permission.READ_CONTACTS,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 101);
            }
        }
    }

}
