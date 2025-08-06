package org.thoughtcrime.securesms.components.emoji;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import org.thoughtcrime.securesms.util.ViewUtil;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.regex.Pattern;


public class AutoScaledEmojiTextView extends AppCompatTextView {

  /*
    source: https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=%5B%3AEmoji%3DYes%3A%5D&g=&i=
    with spaces, *, # and 0-9 removed and the corresponding emojis added explicitly
    to avoid matching normal text with such characters
  */
  private static final Pattern emojiRegex = Pattern.compile("([🏻-🏿😀😃😄😁😆😅🤣😂🙂🙃🫠😉😊😇🥰😍🤩😘😗☺😚😙🥲😋😛😜🤪😝🤑🤗🤭🫢🫣🤫🤔🫡🤐🤨😐😑😶🫥😏😒🙄😬🤥🫨😌😔😪🤤😴😷🤒🤕🤢🤮🤧🥵🥶🥴😵🤯🤠🥳🥸😎🤓🧐😕🫤😟🙁☹😮😯😲😳🥺🥹😦-😨😰😥😢😭😱😖😣😞😓😩😫🥱😤😡😠🤬😈👿💀☠💩🤡👹-👻👽👾🤖😺😸😹😻-😽🙀😿😾🙈-🙊💌💘💝💖💗💓💞💕💟❣💔❤🩷🧡💛💚💙🩵💜🤎🖤🩶🤍💋💯💢💥💫💦💨🕳💬🗨🗯💭💤👋🤚🖐✋🖖🫱-🫴🫷🫸👌🤌🤏✌🤞🫰🤟🤘🤙👈👉👆🖕👇☝🫵👍👎✊👊🤛🤜👏🙌🫶👐🤲🤝🙏✍💅🤳💪🦾🦿🦵🦶👂🦻👃🧠🫀🫁🦷🦴👀👁👅👄🫦👶🧒👦👧🧑👱👨🧔👩🧓👴👵🙍🙎🙅🙆💁🙋🧏🙇🤦🤷👮🕵💂🥷👷🫅🤴👸👳👲🧕🤵👰🤰🫃🫄🤱👼🎅🤶🦸🦹🧙-🧟🧌💆💇🚶🧍🧎🏃💃🕺🕴👯🧖🧗🤺🏇⛷🏂🏌🏄🚣🏊⛹🏋🚴🚵🤸🤼-🤾🤹🧘🛀🛌👭👫👬💏💑🗣👤👥🫂👪👣🦰🦱🦳🦲🐵🐒🦍🦧🐶🐕🦮🐩🐺🦊🦝🐱🐈🦁🐯🐅🐆🐴🫎🫏🐎🦄🦓🦌🦬🐮🐂-🐄🐷🐖🐗🐽🐏🐑🐐🐪🐫🦙🦒🐘🦣🦏🦛🐭🐁🐀🐹🐰🐇🐿🦫🦔🦇🐻🐨🐼🦥🦦🦨🦘🦡🐾🦃🐔🐓🐣-🐧🕊🦅🦆🦢🦉🦤🪶🦩🦚🦜🪽🪿🐸🐊🐢🦎🐍🐲🐉🦕🦖🐳🐋🐬🦭🐟-🐡🦈🐙🐚🪸🪼🐌🦋🐛-🐝🪲🐞🦗🪳🕷🕸🦂🦟🪰🪱🦠💐🌸💮🪷🏵🌹🥀🌺-🌼🌷🪻🌱🪴🌲-🌵🌾🌿☘🍀-🍃🪹🪺🍄🍇-🍍🥭🍎-🍓🫐🥝🍅🫒🥥🥑🍆🥔🥕🌽🌶🫑🥒🥬🥦🧄🧅🥜🫘🌰🫚🫛🍞🥐🥖🫓🥨🥯🥞🧇🧀🍖🍗🥩🥓🍔🍟🍕🌭🥪🌮🌯🫔🥙🧆🥚🍳🥘🍲🫕🥣🥗🍿🧈🧂🥫🍱🍘-🍝🍠🍢-🍥🥮🍡🥟-🥡🦀🦞🦐🦑🦪🍦-🍪🎂🍰🧁🥧🍫-🍯🍼🥛☕🫖🍵🍶🍾🍷-🍻🥂🥃🫗🥤🧋🧃🧉🧊🥢🍽🍴🥄🔪🫙🏺🌍-🌐🗺🗾🧭🏔⛰🌋🗻🏕🏖🏜-🏟🏛🏗🧱🪨🪵🛖🏘🏚🏠-🏦🏨-🏭🏯🏰💒🗼🗽⛪🕌🛕🕍⛩🕋⛲⛺🌁🌃🏙🌄-🌇🌉♨🎠🛝🎡🎢💈🎪🚂-🚊🚝🚞🚋-🚎🚐-🚙🛻🚚-🚜🏎🏍🛵🦽🦼🛺🚲🛴🛹🛼🚏🛣🛤🛢⛽🛞🚨🚥🚦🛑🚧⚓🛟⛵🛶🚤🛳⛴🛥🚢✈🛩🛫🛬🪂💺🚁🚟-🚡🛰🚀🛸🛎🧳⌛⏳⌚⏰-⏲🕰🕛🕧🕐🕜🕑🕝🕒🕞🕓🕟🕔🕠🕕🕡🕖🕢🕗🕣🕘🕤🕙🕥🕚🕦🌑-🌜🌡☀🌝🌞🪐⭐🌟🌠🌌☁⛅⛈🌤-🌬🌀🌈🌂☂☔⛱⚡❄☃⛄☄🔥💧🌊🎃🎄🎆🎇🧨✨🎈-🎋🎍-🎑🧧🎀🎁🎗🎟🎫🎖🏆🏅🥇-🥉⚽⚾🥎🏀🏐🏈🏉🎾🥏🎳🏏🏑🏒🥍🏓🏸🥊🥋🥅⛳⛸🎣🤿🎽🎿🛷🥌🎯🪀🪁🔫🎱🔮🪄🎮🕹🎰🎲🧩🧸🪅🪩🪆♠♥♦♣♟🃏🀄🎴🎭🖼🎨🧵🪡🧶🪢👓🕶🥽🥼🦺👔-👖🧣-🧦👗👘🥻🩱-🩳👙👚🪭👛-👝🛍🎒🩴👞👟🥾🥿👠👡🩰👢🪮👑👒🎩🎓🧢🪖⛑📿💄💍💎🔇-🔊📢📣📯🔔🔕🎼🎵🎶🎙-🎛🎤🎧📻🎷🪗🎸-🎻🪕🥁🪘🪇🪈📱📲☎📞-📠🔋🪫🔌💻🖥🖨⌨🖱🖲💽-📀🧮🎥🎞📽🎬📺📷-📹📼🔍🔎🕯💡🔦🏮🪔📔-📚📓📒📃📜📄📰🗞📑🔖🏷💰🪙💴-💸💳🧾💹✉📧-📩📤-📦📫📪📬-📮🗳✏✒🖋🖊🖌🖍📝💼📁📂🗂📅📆🗒🗓📇-📎🖇📏📐✂🗃🗄🗑🔒🔓🔏-🔑🗝🔨🪓⛏⚒🛠🗡⚔💣🪃🏹🛡🪚🔧🪛🔩⚙🗜⚖🦯🔗⛓🪝🧰🧲🪜⚗🧪-🧬🔬🔭📡💉🩸💊🩹🩼🩺🩻🚪🛗🪞🪟🛏🛋🪑🚽🪠🚿🛁🪤🪒🧴🧷🧹-🧻🪣🧼🫧🪥🧽🧯🛒🚬⚰🪦⚱🧿🪬🗿🪧🪪🏧🚮🚰♿🚹-🚼🚾🛂-🛅⚠🚸⛔🚫🚳🚭🚯🚱🚷📵🔞☢☣⬆↗➡↘⬇↙⬅↖↕↔↩↪⤴⤵🔃🔄🔙-🔝🛐⚛🕉✡☸☯✝☦☪☮🕎🔯🪯♈-♓⛎🔀-🔂▶⏩⏭⏯◀⏪⏮🔼⏫🔽⏬⏸-⏺⏏🎦🔅🔆📶🛜📳📴♀♂⚧✖➕-➗🟰♾‼⁉❓-❕❗〰💱💲⚕♻⚜🔱📛🔰⭕✅☑✔❌❎➰➿〽✳✴❇©®™🔟-🔤🅰🆎🅱🆑-🆓ℹ🆔Ⓜ🆕🆖🅾🆗🅿🆘-🆚🈁🈂🈷🈶🈯🉐🈹🈚🈲🉑🈸🈴🈳㊗㊙🈺🈵🔴🟠-🟢🔵🟣🟤⚫⚪🟥🟧-🟩🟦🟪🟫⬛⬜◼◻◾◽▪▫🔶-🔻💠🔘🔳🔲🏁🚩🎌🏴🏳🇦-🇿\uD83E\uDD89\uD83E\uDD8F\uD83E\uDDBE\uD83E\uDDC6\uD83E\uddcd\uD83E\udddf\uD83E\ude99]|#️⃣|\\*️⃣|0️⃣|1️⃣|2️⃣|3️⃣|4️⃣|5️⃣|6️⃣|7️⃣|8️⃣|9️⃣)+.*");
  private float originalFontSize;

  public AutoScaledEmojiTextView(Context context) {
    this(context, null);
  }

  public AutoScaledEmojiTextView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public AutoScaledEmojiTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    try (TypedArray typedArray = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.textSize})) {
      originalFontSize = ViewUtil.pxToSp(context, typedArray.getDimensionPixelSize(0, 0));
      if (originalFontSize == 0) {
        originalFontSize = 16f;
      }
    }
  }

  @Override
  public void setText(@Nullable CharSequence text, BufferType type) {
    float scale = text != null ? getTextScale(text.toString()) : 1;
    super.setTextSize(TypedValue.COMPLEX_UNIT_SP, originalFontSize * scale);
    super.setText(text, type);
  }

  @Override
  public void setTextSize(float size) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
  }

  @Override
  public void setTextSize(int unit, float size) {
    if (unit == TypedValue.COMPLEX_UNIT_SP) {
      originalFontSize = size;
    } else {
      float pxSize = TypedValue.applyDimension(unit, size, getResources().getDisplayMetrics());
      float spSize = ViewUtil.pxToSp(getContext(), (int) pxSize);
      if (spSize > 0) {
        originalFontSize = spSize;
      }
    }
    super.setTextSize(unit, size);
  }

  private float getTextScale(String text) {
    if (text.length() > 21 || text.isEmpty() || Character.isLetter(text.charAt(0))) {
      return 1;
    }
    int emojiCount = countEmojis(text, 8);
    if (emojiCount <= 0) {
      return 1;
    }

    float scale = 1.25f;
    if (emojiCount <= 6) scale += 0.25f;
    if (emojiCount <= 4) scale += 0.25f;
    if (emojiCount <= 2) scale += 0.25f;
    return scale;
  }

  /**
   * Returns the number of emojis if there are only emojis AND there are no more than `max` emojis,
   * or -1 otherwise.
   */
  public static int countEmojis(String text, int max) {
    BreakIterator graphemeIterator = BreakIterator.getCharacterInstance(Locale.getDefault());

    graphemeIterator.setText(text);

    int graphemeCount = 0;

    // Iterate over the text and count graphemes
    int start = graphemeIterator.first();
    for (int end = graphemeIterator.next(); end != BreakIterator.DONE; start = end, end = graphemeIterator.next()) {
      String grapheme = text.substring(start, end);
      if (!emojiRegex.matcher(grapheme).matches()) return -1;
      if (++graphemeCount > max) return -1;
    }

    return graphemeCount;
  }
}
