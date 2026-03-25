package com.kamwithk.ankiconnectandroid.routing.localaudiosource;

import com.kamwithk.ankiconnectandroid.routing.database.Entry;

public class OZK5AudioSource extends LocalAudioSource {
    public OZK5AudioSource() {
        super("ozk5", "user_files/ozk5_files");
    }

    @Override
    public String getSourceName(Entry entry) {
        return "OZK5";
    }
}
