package org.thoughtcrime.securesms.search.model;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcChatlist;

/**
 * Represents an all-encompassing search result that can contain various result for different
 * subcategories.
 */
public class SearchResult {

  public static final SearchResult EMPTY = new SearchResult("", new int[]{}, new DcChatlist(0, 0), new int[]{});

  private final String     query;
  private final int[]      contacts;
  private final DcChatlist conversations;
  private final int[]      messages;

  public SearchResult(@NonNull String     query,
                      @NonNull int[]      contacts,
                      @NonNull DcChatlist conversations,
                      @NonNull int[]      messages)
  {
    this.query         = query;
    this.contacts      = contacts;
    this.conversations = conversations;
    this.messages      = messages;
  }

  public int[] getContacts() {
    return contacts;
  }

  public DcChatlist getChats() {
    return conversations;
  }

  public int[] getMessages() {
    return messages;
  }

  public String getQuery() {
    return query;
  }

  public int size() {
    return contacts.length + conversations.getCnt() + messages.length;
  }

  public boolean isEmpty() {
    return size() == 0;
  }
}
