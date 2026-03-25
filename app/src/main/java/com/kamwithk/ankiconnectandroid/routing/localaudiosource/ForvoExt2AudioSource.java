package com.kamwithk.ankiconnectandroid.routing.localaudiosource;

import com.kamwithk.ankiconnectandroid.routing.database.Entry;

public class ForvoExt2AudioSource extends LocalAudioSource {
    public ForvoExt2AudioSource() {
        super("forvo_ext2", "user_files/forvo_ext2_files");
    }

    @Override
    public String getSourceName(Entry entry)  {
        return "Forvo Ext2";
    }
}
