/* Autogenerated file, do not edit manually */
package chat.delta.rpc.types;

public class FullChat {
  public Boolean archived;
  public Boolean canSend;
  public Integer chatType;
  public String color;
  public java.util.List<Integer> contactIds;
  public java.util.List<Contact> contacts;
  public Integer ephemeralTimer;
  public Integer freshMessageCounter;
  public Integer id;
  public Boolean isContactRequest;
  public Boolean isDeviceChat;
  public Boolean isMuted;
  /**
   * True if the chat is protected.
   *
   * UI should display a green checkmark in the chat title, in the chat profile title and in the chatlist item if chat protection is enabled. UI should also display a green checkmark in the contact profile if 1:1 chat with this contact exists and is protected.
   */
  public Boolean isProtected;
  public Boolean isProtectionBroken;
  public Boolean isSelfTalk;
  public Boolean isUnpromoted;
  @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public String mailingListAddress;
  public String name;
  @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public String profileImage;
  public Boolean selfInGroup;
  public Boolean wasSeenRecently;
}