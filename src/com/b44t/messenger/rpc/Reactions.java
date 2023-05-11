package com.b44t.messenger.rpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Reactions {
    // Map from a contact to it's reaction to message.
    private final HashMap<Integer, String[]> reactionsByContact;
    // Unique reactions, sorted in descending order.
    private final ArrayList<Reaction> reactions;

    public Reactions(HashMap<Integer, String[]> reactionsByContact, ArrayList<Reaction> reactions) {
        this.reactionsByContact = reactionsByContact;
        this.reactions = reactions;
    }

    public Map<Integer, String[]> getReactionsByContact() {
        return reactionsByContact;
    }

    public List<Reaction> getReactions() {
        return reactions;
    }
}
