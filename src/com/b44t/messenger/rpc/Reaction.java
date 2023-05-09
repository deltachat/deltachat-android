package com.b44t.messenger.rpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class Reaction {
    // The reaction text string.
    private final String text;
    // The count of users that have reacted with this reaction.
    private final int count;

    public Reaction(String text, int count) {
        this.text = text;
        this.count = count;
    }

    public String getText() {
        return text;
    }

    public int getCount() {
        return count;
    }

    public static class ReactionDeserializer implements JsonDeserializer<Reaction> {
        @Override
        public Reaction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonArray tuple = json.getAsJsonArray();
            return new Reaction(tuple.get(0).getAsString(), tuple.get(1).getAsInt());
        }
    }
}
