package com.kamwithk.ankiconnectandroid.routing.localaudiosource;

import com.kamwithk.ankiconnectandroid.routing.database.Entry;

public class DaijisenAudioSource extends LocalAudioSource {
    public DaijisenAudioSource() {
        super("daijisen", "user_files/daijisen_files");
    }

    @Override
    public String getSourceName(Entry entry) {
        return "Daijisen " + entry.display;
    }
}
