package com.kamwithk.ankiconnectandroid.routing.localaudiosource;

import com.kamwithk.ankiconnectandroid.routing.database.Entry;

public class ForvoExtAudioSource extends LocalAudioSource {
    public ForvoExtAudioSource() {
        super("forvo_ext", "user_files/forvo_ext_files");
    }

    @Override
    public String getSourceName(Entry entry)  {
        return "Forvo Ext";
    }
}
