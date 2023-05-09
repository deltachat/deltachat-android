package com.b44t.messenger.rpc;

import java.util.HashMap;

public class Reactions {
    // Map from a contact to it's reaction to message.
    private HashMap<Integer, String[]> reactionsByContact;
    // Unique reactions, sorted in descending order.
    private Reaction[] reactions;

    public Reactions(HashMap<Integer, String[]> reactionsByContact, Reaction[] reactions) {
        this.reactionsByContact = reactionsByContact;
        this.reactions = reactions;
    }

    public HashMap<Integer, String[]> getReactionsByContact() {
        return reactionsByContact;
    }

    public Reaction[] getReactions() {
        return reactions;
    }
}
