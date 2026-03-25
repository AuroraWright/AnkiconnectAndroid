package com.kamwithk.ankiconnectandroid.routing.localaudiosource;

import com.kamwithk.ankiconnectandroid.routing.database.Entry;

public class TAASAudioSource extends LocalAudioSource {
    public TAASAudioSource() {
        super("taas", "user_files/taas_files");
    }

    @Override
    public String getSourceName(Entry entry) {
        return "TAAS";
    }
}
