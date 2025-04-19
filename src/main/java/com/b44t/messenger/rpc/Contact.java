package com.b44t.messenger.rpc;

public class Contact {
    public final String address;
    public final String color;
    public final String authName;
    public final String status;
    public final String displayName;
    public final int id;
    public final String name;
    public final String profileImage;
    public final String nameAndAddr;
    public final boolean isBlocked;
    public final boolean isPgpContact;
    public final boolean e2eeAvail;

    /// True if the contact can be added to verified groups.
    ///
    /// If this is true
    /// UI should display green checkmark after the contact name
    /// in contact list items,
    /// in chat member list items
    /// and in profiles if no chat with the contact exist.
    public final boolean isVerified;

    /// True if the contact profile title should have a green checkmark.
    ///
    /// This indicates whether 1:1 chat has a green checkmark
    /// or will have a green checkmark if created.
    public final boolean isProfileVerified;

    /// The ID of the contact that verified this contact.
    ///
    /// If this is present,
    /// display a green checkmark and "Introduced by ..."
    /// string followed by the verifier contact name and address
    /// in the contact profile.
    public final int verifierId;

    /// the contact's last seen timestamp
    public final float lastSeen;
    public final boolean wasSeenRecently;

    /// If the contact is a bot.
    public final boolean isBot;

    public Contact(
                   String address,
                   String color,
                   String authName,
                   String status,
                   String displayName,
                   int id,
                   String name,
                   String profileImage,
                   String nameAndAddr,
                   boolean isBlocked,
                   boolean isPgpContact,
                   boolean e2eeAvail,
                   boolean isVerified,
                   boolean isProfileVerified,
                   int verifierId,
                   float lastSeen,
                   boolean wasSeenRecently,
                   boolean isBot
                   ) {
        this.address = address;
        this.color = color;
        this.authName = authName;
        this.status = status;
        this.displayName = displayName;
        this.id = id;
        this.name = name;
        this.profileImage = profileImage;
        this.nameAndAddr = nameAndAddr;
        this.isBlocked = isBlocked;
        this.isPgpContact = isPgpContact;
        this.e2eeAvail = e2eeAvail;
        this.isVerified = isVerified;
        this.isProfileVerified = isProfileVerified;
        this.verifierId = verifierId;
        this.lastSeen = lastSeen;
        this.wasSeenRecently = wasSeenRecently;
        this.isBot = isBot;
    }
}
