package com.locbot;

import java.util.HashMap;

/**
 * Created by santanu on 19/4/17.
 */

public class AllAppData {
    private static HashMap<String, String> allContacts;

    public static HashMap<String, String> getAllContacts() {
        return allContacts;
    }

    public static void setAllContacts(HashMap<String, String> allContacts) {
        AllAppData.allContacts = allContacts;
    }

    public static String userName = "uname";
    public static String userMobileNumber  = "uphno";
}
