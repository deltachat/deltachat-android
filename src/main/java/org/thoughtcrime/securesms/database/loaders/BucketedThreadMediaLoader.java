package org.thoughtcrime.securesms.database.loaders;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.loader.content.AsyncTaskLoader;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BucketedThreadMediaLoader extends AsyncTaskLoader<BucketedThreadMediaLoader.BucketedThreadMedia> {

  private final int chatId;
  private final int msgType1;
  private final int msgType2;
  private final int msgType3;

  public BucketedThreadMediaLoader(@NonNull Context context, int chatId, int msgType1, int msgType2, int msgType3) {
    super(context);
    this.chatId = chatId;
    this.msgType1 = msgType1;
    this.msgType2 = msgType2;
    this.msgType3 = msgType3;

    onContentChanged();
  }

  @Override
  protected void onStartLoading() {
    if (takeContentChanged()) {
      forceLoad();
    }
  }

  @Override
  protected void onStopLoading() {
    cancelLoad();
  }

  @Override
  protected void onAbandon() {
  }

  @Override
  public BucketedThreadMedia loadInBackground() {
    BucketedThreadMedia result   = new BucketedThreadMedia(getContext());
    DcContext context = DcHelper.getContext(getContext());
    if(chatId!=-1 /*0=all, -1=none*/) {
      int[] messages = context.getChatMedia(chatId, msgType1, msgType2, msgType3);
      for(int nextId : messages) {
        result.add(context.getMsg(nextId));
      }
    }

    return result;
  }

  public static class BucketedThreadMedia {

    private final TimeBucket   TODAY;
    private final TimeBucket   YESTERDAY;
    private final TimeBucket   THIS_WEEK;
    private final TimeBucket   LAST_WEEK;
    private final TimeBucket   THIS_MONTH;
    private final TimeBucket   LAST_MONTH;
    private final MonthBuckets OLDER;

    private final TimeBucket[] TIME_SECTIONS;

    public BucketedThreadMedia(@NonNull Context context) {
      // from today midnight until the end of human time
      this.TODAY         = new TimeBucket(context.getString(R.string.today),
          addToCalendarFromTodayMidnight(Calendar.DAY_OF_YEAR, 0), Long.MAX_VALUE);
      // from yesterday midnight until today midnight
      this.YESTERDAY     = new TimeBucket(context.getString(R.string.yesterday),
          addToCalendarFromTodayMidnight(Calendar.DAY_OF_YEAR, -1), TODAY.startTime);
      // from the closest start of week until yesterday midnight (that can be a negative timespace and thus be empty)
      this.THIS_WEEK     = new TimeBucket(context.getString(R.string.this_week),
          setInCalendarFromTodayMidnight(Calendar.DAY_OF_WEEK, getCalendar().getFirstDayOfWeek()), YESTERDAY.startTime);
      // from the closest start of week one week back until the closest start of week.
      this.LAST_WEEK     = new TimeBucket(context.getString(R.string.last_week),
          addToCalendarFrom(THIS_WEEK.startTime, Calendar.WEEK_OF_YEAR, -1), THIS_WEEK.startTime);
      // from the closest 1st of a month until one week prior to the closest start of week (can be negative and thus empty)
      this.THIS_MONTH    = new TimeBucket(context.getString(R.string.this_month),
          setInCalendarFromTodayMidnight(Calendar.DAY_OF_MONTH, 1), LAST_WEEK.startTime);
      // from the closest 1st of a month, one month back to the closest 1st of a month
      this.LAST_MONTH    = new TimeBucket(context.getString(R.string.last_month),
          addToCalendarFrom(THIS_MONTH.startTime, Calendar.MONTH, -1), LAST_WEEK.startTime);
      this.TIME_SECTIONS = new TimeBucket[]{TODAY, YESTERDAY, THIS_WEEK, LAST_WEEK, THIS_MONTH, LAST_MONTH};
      this.OLDER         = new MonthBuckets();
    }

    public void add(DcMsg imageMessage) {
      for (TimeBucket timeSection : TIME_SECTIONS) {
        if (timeSection.inRange(imageMessage.getTimestamp())) {
          timeSection.add(imageMessage);
          return;
        }
      }
      OLDER.add(imageMessage);
    }

    public LinkedList<DcMsg> getAll() {
      LinkedList<DcMsg> messages = new LinkedList<>();
      for (TimeBucket section : TIME_SECTIONS) {
        messages.addAll(section.records);
      }
      for (List<DcMsg> records : OLDER.months.values()) {
        messages.addAll(records);
      }
      return messages;
    }

    public int getSectionCount() {
      int count = 0;
      for (TimeBucket section : TIME_SECTIONS) {
        if (!section.isEmpty()) count++;
      }
      return count + OLDER.getSectionCount();
    }

    public int getSectionItemCount(int section) {
      List<TimeBucket> activeTimeBuckets = new ArrayList<>();
      for (TimeBucket bucket : TIME_SECTIONS) {
        if (!bucket.isEmpty()) activeTimeBuckets.add(bucket);
      }

      if (section < activeTimeBuckets.size()) return activeTimeBuckets.get(section).getItemCount();
      else                                    return OLDER.getSectionItemCount(section - activeTimeBuckets.size());
    }

    public DcMsg get(int section, int item) {
      List<TimeBucket> activeTimeBuckets = new ArrayList<>();
      for (TimeBucket bucket : TIME_SECTIONS) {
        if (!bucket.isEmpty()) activeTimeBuckets.add(bucket);
      }

      if (section < activeTimeBuckets.size()) return activeTimeBuckets.get(section).getItem(item);
      else                                    return OLDER.getItem(section - activeTimeBuckets.size(), item);
    }

    public String getName(int section) {
      List<TimeBucket> activeTimeBuckets = new ArrayList<>();
      for (TimeBucket bucket : TIME_SECTIONS) {
        if (!bucket.isEmpty()) activeTimeBuckets.add(bucket);
      }

      if (section < activeTimeBuckets.size()) return activeTimeBuckets.get(section).getName();
      else                                    return OLDER.getName(section - activeTimeBuckets.size());
    }

    // tests should override this function to deliver a preset calendar.
    Calendar getCalendar() {
      return Calendar.getInstance();
    }

    long setInCalendarFromTodayMidnight(int field, int amount) {
      Calendar calendar = getCalendar();
      setCalendarToTodayMidnight(calendar);
      calendar.set(field, amount);
      return calendar.getTimeInMillis();
    }

    long addToCalendarFrom(long relativeTo, int field, int amount) {
      Calendar calendar = getCalendar();
      calendar.setTime(new Date(relativeTo));
      calendar.add(field, amount);
      return calendar.getTimeInMillis();
    }

    long addToCalendarFromTodayMidnight(int field, int amount) {
      Calendar calendar = getCalendar();
      setCalendarToTodayMidnight(calendar);
      calendar.add(field, amount);
      return calendar.getTimeInMillis();
    }

    void setCalendarToTodayMidnight(Calendar calendar) {
      calendar.set(Calendar.HOUR_OF_DAY, 0);
      calendar.set(Calendar.MINUTE, 0);
      calendar.set(Calendar.SECOND, 0);
      calendar.set(Calendar.MILLISECOND, 0);
    }

    private static class TimeBucket {

      private final LinkedList<DcMsg> records = new LinkedList<>();

      private final long   startTime;
      private final long endTime;
      private final String name;

      TimeBucket(String name, long startTime, long endTime) {
        this.name      = name;
        this.startTime = startTime;
        this.endTime = endTime;
      }

      void add(DcMsg record) {
        this.records.addFirst(record);
      }

      boolean inRange(long timestamp) {
        return timestamp >= startTime && timestamp < endTime;
      }

      boolean isEmpty() {
        return records.isEmpty();
      }

      int getItemCount() {
        return records.size();
      }

      DcMsg getItem(int position) {
        return records.get(position);
      }

      String getName() {
        return name;
      }
    }

    private static class MonthBuckets {

      private final Map<Date, LinkedList<DcMsg>> months = new HashMap<>();

      void add(DcMsg record) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(record.getTimestamp());

        int  year  = calendar.get(Calendar.YEAR) - 1900;
        int  month = calendar.get(Calendar.MONTH);
        Date date  = new Date(year, month, 1);

        if (months.containsKey(date)) {
          months.get(date).addFirst(record);
        } else {
          LinkedList<DcMsg> list = new LinkedList<>();
          list.add(record);
          months.put(date, list);
        }
      }

      int getSectionCount() {
        return months.size();
      }

      int getSectionItemCount(int section) {
        return months.get(getSection(section)).size();
      }

      DcMsg getItem(int section, int position) {
        return months.get(getSection(section)).get(position);
      }

      Date getSection(int section) {
        ArrayList<Date> keys = new ArrayList<>(months.keySet());
        Collections.sort(keys, Collections.reverseOrder());

        return keys.get(section);
      }

      String getName(int section) {
        Date sectionDate = getSection(section);

        return new SimpleDateFormat("MMMM yyyy", Util.getLocale()).format(sectionDate);
      }
    }
  }
}
