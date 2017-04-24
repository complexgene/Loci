package com.locbot;

/**
 * Created by santanu on 18/04/17.
 */

public class SingleTonUser {
    private String mobileNumber;
    private String name;
    private static SingleTonUser singleTonUser;

    private  SingleTonUser(){

    }

    public static SingleTonUser getInstance(){
        if(singleTonUser == null){
            singleTonUser = new SingleTonUser();
        }
        return singleTonUser;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
