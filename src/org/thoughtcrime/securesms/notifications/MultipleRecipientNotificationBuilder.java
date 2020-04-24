package org.thoughtcrime.securesms.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.preferences.widgets.NotificationPrivacyPreference;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MultipleRecipientNotificationBuilder extends AbstractNotificationBuilder {

    private final LinkedList<MessageBody> messageBodies = new LinkedList<>();

    private static class MessageBody {
        final @Nullable
        Recipient group;
        final @NonNull
        Recipient sender;
        final @NonNull
        CharSequence message;

        MessageBody(@Nullable Recipient group, @NonNull Recipient sender, @NonNull CharSequence message) {
            this.group = group;
            this.sender = sender;
            this.message = message;
        }
    }

    MultipleRecipientNotificationBuilder(Context context, NotificationPrivacyPreference privacy) {
        super(context, privacy);

        setColor(context.getResources().getColor(R.color.delta_primary));
        setSmallIcon(R.drawable.icon_notification);
        setContentTitle(context.getString(R.string.app_name));
        setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, ConversationListActivity.class), 0));
        setCategory(NotificationCompat.CATEGORY_MESSAGE);
        setPriority(Prefs.getNotificationPriority(context));
        setGroupSummary(true);
    }

    void setMessageCount(int messageCount, int chatCount) {
        setSubText(context.getString(R.string.notify_n_messages_in_m_chats,
                messageCount, chatCount));
        setContentInfo(String.valueOf(messageCount));
        setNumber(messageCount); // this also sets badges on android8 and newer
    }

    void setMostRecentSender(Recipient recipient) {
        if (privacy.isDisplayContact()) {
            setContentText(context.getString(R.string.notify_most_recent_from,
                    recipient.toShortString()));
        }
    }

    void addActions(PendingIntent markAsReadIntent) {
        NotificationCompat.Action markAllAsReadAction = new NotificationCompat.Action(R.drawable.check,
                context.getString(R.string.notify_mark_all_read),
                markAsReadIntent);
        addAction(markAllAsReadAction);
        extend(new NotificationCompat.WearableExtender().addAction(markAllAsReadAction));
    }

    void addMessageBody(@Nullable Recipient group, @NonNull Recipient sender, @Nullable CharSequence body) {
        messageBodies.add(new MessageBody(group, sender, body));

        if (privacy.isDisplayContact() && sender.getContactUri() != null) {
            addPerson(sender.getContactUri().toString());
        } else if (privacy.isDisplayContact() && group != null && group.getContactUri() != null) {
            addPerson(group.getContactUri().toString());
        }
    }

    @Override
    public Notification build() {
        if (privacy.isDisplayMessage() || privacy.isDisplayContact()) {
            NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
            Map<Recipient, List<MessageBody>> byGroup = new LinkedHashMap<>();
            for (MessageBody body : messageBodies) {
                Recipient key = body.group == null ? body.sender : body.group;
                if (byGroup.containsKey(key)) {
                    LinkedList<MessageBody> messagebodies = (LinkedList<MessageBody>) byGroup.remove(key);
                    messagebodies.addFirst(body);
                    byGroup.put(key, messagebodies);
                } else {
                    byGroup.put(key, new LinkedList<>());
                    byGroup.get(key).add(body);
                }
            }

            if (privacy.isDisplayMessage()) {
                LinkedList<Recipient> list = new LinkedList<>(byGroup.keySet());
                Iterator<Recipient> iterator = list.descendingIterator();
                while (iterator.hasNext()) {
                    Recipient nextGroup = iterator.next();
                    String groupName = nextGroup.getName();
                    List<MessageBody> messages = byGroup.get(nextGroup);
                    String firstMessageSender = messages.get(0).sender.getName();
                    if (groupName != null && groupName.equals(firstMessageSender)) { // individual
                        for (MessageBody body : messages) {
                            style.addLine(getStyledMessage(body.sender, body.message));
                        }
                    } else { // group chat
                        style.addLine(Util.getBoldedString(groupName));
                        for (MessageBody body : messages) {
                            style.addLine("- " + getStyledMessage(body.sender, body.message));
                        }
                    }
                }
            } else if (privacy.isDisplayContact()) {
                LinkedList<Recipient> list = new LinkedList<>(byGroup.keySet());
                Iterator<Recipient> iterator = list.descendingIterator();
                while (iterator.hasNext()) {
                    Recipient nextGroup = iterator.next();
                    String groupName = nextGroup.getName();
                    List<MessageBody> messages = byGroup.get(nextGroup);
                    String firstMessageSender = messages.get(0).sender.getName();
                    if (groupName != null && groupName.equals(firstMessageSender)) { // individual
                        for (MessageBody body : messages) {
                            style.addLine(body.sender.getName());
                        }
                    } else { // group chat
                        style.addLine(Util.getBoldedString(groupName));
                        for (MessageBody body : messages) {
                            style.addLine("- " + body.sender.getName());
                        }
                    }
                }
            }

            setStyle(style);
        }

        return super.build();
    }
}
