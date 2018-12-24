package com.coltennye.punctual;

import android.app.Application;
import android.util.Log;


import com.coltennye.punctual.db.MyObjectBox;

import io.objectbox.BoxStore;
import io.objectbox.android.AndroidObjectBrowser;

public class App extends Application {

    private BoxStore boxStore;

    @Override
    public void onCreate() {
        super.onCreate();
        TimeConverter.init(this);
        boxStore = MyObjectBox.builder().androidContext(App.this).build();
        //boxStore.close();
        //boxStore.deleteAllFiles();
        if (BuildConfig.DEBUG) {
            new AndroidObjectBrowser(boxStore).start(this);
        }


        Log.d("punctual.App", "Using ObjectBox " + BoxStore.getVersion() + " (" + BoxStore.getVersionNative() + ")");
    }


    public BoxStore getBoxStore() {
        return boxStore;
    }
}
