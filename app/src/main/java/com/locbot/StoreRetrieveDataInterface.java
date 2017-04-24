package com.locbot;

import java.io.IOException;

/**
 * Created by snaskar on 10/10/2016.
 */

public interface StoreRetrieveDataInterface {
    public void beginWriteTransaction();
    public void beginReadTransaction();

    public boolean fileExists();
    public String getValueFor(String key);
    public void updateValueFor(String key, String newValue);
    public void createNewData(String key, String value) throws IOException;
    public void removeData(String key);

    public void endWriteTransaction();
    public void endReadTransaction();
}
