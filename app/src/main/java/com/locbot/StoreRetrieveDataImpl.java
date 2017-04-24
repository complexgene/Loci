package com.locbot;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by snaskar on 10/10/2016.
 */

public class StoreRetrieveDataImpl implements StoreRetrieveDataInterface {
    private Context context;
    private String fileName;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private Properties prop = new Properties();
    private File storeRetrieveDirectory;
    private File storeRetrieveFile;
    private HashMap<String,String> allKeysAndValues = new HashMap<>();

    public StoreRetrieveDataImpl(String fileName){
        try {
            this.fileName = fileName ;
        }
        catch(Exception e){
            Log.e("StoreRetrieveData_Err",e.getMessage());
            e.printStackTrace();
        }
    }

    // Check if the user details file exists or not, return TRUE if it exists or FALSE if it doesn't.
    public boolean fileExists(){
        File f = new File(Environment.getExternalStorageDirectory().getAbsoluteFile().toString() + "/locbot/"+fileName);
        return f.exists();
    }

    // GET, SET, make the code ready to initialize things before you start interacting with the file
    //----------------------------------------------------------------------------------------------
    public void beginTransaction(){
        try {
            storeRetrieveDirectory = new File(Environment.getExternalStorageDirectory().getAbsoluteFile().toString() + "/locbot/");
            storeRetrieveDirectory.mkdirs();
            storeRetrieveFile = new File(storeRetrieveDirectory, fileName);
            if(storeRetrieveFile.exists()) {
                bufferedReader = new BufferedReader(new FileReader(storeRetrieveFile));
                String eachLineRead = "";
                while ((eachLineRead = bufferedReader.readLine()) != null) {
                    String[] keyAndValue = eachLineRead.split("=");
                    allKeysAndValues.put(keyAndValue[0], keyAndValue[1]);
                }
            }
        }
        catch(Exception e){
            Toast.makeText(context, "Exeee:"+e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    public void beginWriteTransaction() {
        try {
            beginReadTransaction();
        } catch (Exception e) {
            Toast.makeText(context, "Exeee:"+e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    public void beginReadTransaction() {
        try {
            beginTransaction();
        } catch (Exception e) {
            Toast.makeText(context, "Exeee:"+e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // All the helper methods to interact with the file
    //----------------------------------------------------------------------------------------------
    public String getValueFor(String key) {
        return allKeysAndValues.get(key);
    }

    public void updateValueFor(String key, String newValue) {
        allKeysAndValues.put(key, newValue);
    }

    public void createNewData(String key, String value){
        allKeysAndValues.put(key,value);
    }

    public void removeData(String key){
        allKeysAndValues.remove(key);
    }

    // Finshed with all the helper methods that will interact with the file
    //----------------------------------------------------------------------------------------------
    public void endWriteTransaction(){
        try{
            bufferedWriter = new BufferedWriter(new FileWriter(storeRetrieveFile));
            for(String key:allKeysAndValues.keySet()){
                bufferedWriter.write(key+"="+allKeysAndValues.get(key)+"\n");
            }
            bufferedWriter.close();
            endReadTransaction();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public void endReadTransaction(){
        try{
            bufferedReader.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    //-------------------------------------------------ENCRYPT----//
    public String returnEncrypted(String data){
        char[] allChars = data.toCharArray();
        for(int i=0;i<allChars.length;i++){
            allChars[i]+=5;
        }
        return String.valueOf(allChars);
    }
    public String returnDecrypted(String data){
        char[] allChars = data.toCharArray();
        for(int i=0;i<allChars.length;i++){
            allChars[i]-=5;
        }
        Toast.makeText(context, "Here:"+ String.valueOf(allChars), Toast.LENGTH_LONG).show();
        return String.valueOf(allChars);
    }
}
