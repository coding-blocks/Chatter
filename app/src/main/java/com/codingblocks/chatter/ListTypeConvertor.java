package com.codingblocks.chatter;

import android.arch.persistence.room.TypeConverter;

import com.codingblocks.chatter.db.Mentions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;

public class ListTypeConvertor implements Serializable {

    @TypeConverter // note this annotation
    public String fromOptionValuesList(List<Mentions> optionValues) {
        if (optionValues == null) {
            return (null);
        }
        Gson gson = new Gson();
        Type type = new TypeToken<List<Mentions>>() {
        }.getType();
        String json = gson.toJson(optionValues, type);
        return json;
    }

    @TypeConverter // note this annotation
    public List<Mentions> toOptionValuesList(String optionValuesString) {
        if (optionValuesString == null) {
            return (null);
        }
        Gson gson = new Gson();
        Type type = new TypeToken<List<Mentions>>() {
        }.getType();
        List<Mentions> mentionsList = gson.fromJson(optionValuesString, type);
        return mentionsList;
    }

}
