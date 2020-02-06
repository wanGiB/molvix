package com.molvix.android.models;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@SuppressWarnings("WeakerAccess")
@Entity
public class Presets {
    @Id
    public long id;
    public String presetString;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPresetString() {
        return presetString;
    }

    public void setPresetString(String presetString) {
        this.presetString = presetString;
    }
}
