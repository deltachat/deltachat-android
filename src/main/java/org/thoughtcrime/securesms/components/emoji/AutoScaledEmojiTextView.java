package org.thoughtcrime.securesms.components.emoji;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.regex.Pattern;


public class AutoScaledEmojiTextView extends AppCompatTextView {

  /*
    source: https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=%5B%3AEmoji%3DYes%3A%5D&g=&i=
    with spaces, *, # and 0-9 removed and the corresponding emojis added explicitly
    to avoid matching normal text with such characters
  */
  private static final Pattern emojiRegex = Pattern.compile("([🏻-🏿😀😃😄😁😆😅🤣😂🙂🙃🫠😉😊😇🥰😍🤩😘😗☺😚😙🥲😋😛😜🤪😝🤑🤗🤭🫢🫣🤫🤔🫡🤐🤨😐😑😶🫥😏😒🙄😬🤥🫨😌😔😪🤤😴😷🤒🤕🤢🤮🤧🥵🥶🥴😵🤯🤠🥳🥸😎🤓🧐😕🫤😟🙁☹😮😯😲😳🥺🥹😦-😨😰😥😢😭😱😖😣😞😓😩😫🥱😤😡😠🤬😈👿💀☠💩🤡👹-👻👽👾🤖😺😸😹😻-😽🙀😿😾🙈-🙊💌💘💝💖💗💓💞💕💟❣💔❤🩷🧡💛💚💙🩵💜🤎🖤🩶🤍💋💯💢💥💫💦💨🕳💬🗨🗯💭💤👋🤚🖐✋🖖🫱-🫴🫷🫸👌🤌🤏✌🤞🫰🤟🤘🤙👈👉👆🖕👇☝🫵👍👎✊👊🤛🤜👏🙌🫶👐🤲🤝🙏✍💅🤳💪🦾🦿🦵🦶👂🦻👃🧠🫀🫁🦷🦴👀👁👅👄🫦👶🧒👦👧🧑👱👨🧔👩🧓👴👵🙍🙎🙅🙆💁🙋🧏🙇🤦🤷👮🕵💂🥷👷🫅🤴👸👳👲🧕🤵👰🤰🫃🫄🤱👼🎅🤶🦸🦹🧙-🧟🧌💆💇🚶🧍🧎🏃💃🕺🕴👯🧖🧗🤺🏇⛷🏂🏌🏄🚣🏊⛹🏋🚴🚵🤸🤼-🤾🤹🧘🛀🛌👭👫👬💏💑🗣👤👥🫂👪👣🦰🦱🦳🦲🐵🐒🦍🦧🐶🐕🦮🐩🐺🦊🦝🐱🐈🦁🐯🐅🐆🐴🫎🫏🐎🦄🦓🦌🦬🐮🐂-🐄🐷🐖🐗🐽🐏🐑🐐🐪🐫🦙🦒🐘🦣🦏🦛🐭🐁🐀🐹🐰🐇🐿🦫🦔🦇🐻🐨🐼🦥🦦🦨🦘🦡🐾🦃🐔🐓🐣-🐧🕊🦅🦆🦢🦉🦤🪶🦩🦚🦜🪽🪿🐸🐊🐢🦎🐍🐲🐉🦕🦖🐳🐋🐬🦭🐟-🐡🦈🐙🐚🪸🪼🐌🦋🐛-🐝🪲🐞🦗🪳🕷🕸🦂🦟🪰🪱🦠💐🌸💮🪷🏵🌹🥀🌺-🌼🌷🪻🌱🪴🌲-🌵🌾🌿☘🍀-🍃🪹🪺🍄🍇-🍍🥭🍎-🍓🫐🥝🍅🫒🥥🥑🍆🥔🥕🌽🌶🫑🥒🥬🥦🧄🧅🥜🫘🌰🫚🫛🍞🥐🥖🫓🥨🥯🥞🧇🧀🍖🍗🥩🥓🍔🍟🍕🌭🥪🌮🌯🫔🥙🧆🥚🍳🥘🍲🫕🥣🥗🍿🧈🧂🥫🍱🍘-🍝🍠🍢-🍥🥮🍡🥟-🥡🦀🦞🦐🦑🦪🍦-🍪🎂🍰🧁🥧🍫-🍯🍼🥛☕🫖🍵🍶🍾🍷-🍻🥂🥃🫗🥤🧋🧃🧉🧊🥢🍽🍴🥄🔪🫙🏺🌍-🌐🗺🗾🧭🏔⛰🌋🗻🏕🏖🏜-🏟🏛🏗🧱🪨🪵🛖🏘🏚🏠-🏦🏨-🏭🏯🏰💒🗼🗽⛪🕌🛕🕍⛩🕋⛲⛺🌁🌃🏙🌄-🌇🌉♨🎠🛝🎡🎢💈🎪🚂-🚊🚝🚞🚋-🚎🚐-🚙🛻🚚-🚜🏎🏍🛵🦽🦼🛺🚲🛴🛹🛼🚏🛣🛤🛢⛽🛞🚨🚥🚦🛑🚧⚓🛟⛵🛶🚤🛳⛴🛥🚢✈🛩🛫🛬🪂💺🚁🚟-🚡🛰🚀🛸🛎🧳⌛⏳⌚⏰-⏲🕰🕛🕧🕐🕜🕑🕝🕒🕞🕓🕟🕔🕠🕕🕡🕖🕢🕗🕣🕘🕤🕙🕥🕚🕦🌑-🌜🌡☀🌝🌞🪐⭐🌟🌠🌌☁⛅⛈🌤-🌬🌀🌈🌂☂☔⛱⚡❄☃⛄☄🔥💧🌊🎃🎄🎆🎇🧨✨🎈-🎋🎍-🎑🧧🎀🎁🎗🎟🎫🎖🏆🏅🥇-🥉⚽⚾🥎🏀🏐🏈🏉🎾🥏🎳🏏🏑🏒🥍🏓🏸🥊🥋🥅⛳⛸🎣🤿🎽🎿🛷🥌🎯🪀🪁🔫🎱🔮🪄🎮🕹🎰🎲🧩🧸🪅🪩🪆♠♥♦♣♟🃏🀄🎴🎭🖼🎨🧵🪡🧶🪢👓🕶🥽🥼🦺👔-👖🧣-🧦👗👘🥻🩱-🩳👙👚🪭👛-👝🛍🎒🩴👞👟🥾🥿👠👡🩰👢🪮👑👒🎩🎓🧢🪖⛑📿💄💍💎🔇-🔊📢📣📯🔔🔕🎼🎵🎶🎙-🎛🎤🎧📻🎷🪗🎸-🎻🪕🥁🪘🪇🪈📱📲☎📞-📠🔋🪫🔌💻🖥🖨⌨🖱🖲💽-📀🧮🎥🎞📽🎬📺📷-📹📼🔍🔎🕯💡🔦🏮🪔📔-📚📓📒📃📜📄📰🗞📑🔖🏷💰🪙💴-💸💳🧾💹✉📧-📩📤-📦📫📪📬-📮🗳✏✒🖋🖊🖌🖍📝💼📁📂🗂📅📆🗒🗓📇-📎🖇📏📐✂🗃🗄🗑🔒🔓🔏-🔑🗝🔨🪓⛏⚒🛠🗡⚔💣🪃🏹🛡🪚🔧🪛🔩⚙🗜⚖🦯🔗⛓🪝🧰🧲🪜⚗🧪-🧬🔬🔭📡💉🩸💊🩹🩼🩺🩻🚪🛗🪞🪟🛏🛋🪑🚽🪠🚿🛁🪤🪒🧴🧷🧹-🧻🪣🧼🫧🪥🧽🧯🛒🚬⚰🪦⚱🧿🪬🗿🪧🪪🏧🚮🚰♿🚹-🚼🚾🛂-🛅⚠🚸⛔🚫🚳🚭🚯🚱🚷📵🔞☢☣⬆↗➡↘⬇↙⬅↖↕↔↩↪⤴⤵🔃🔄🔙-🔝🛐⚛🕉✡☸☯✝☦☪☮🕎🔯🪯♈-♓⛎🔀-🔂▶⏩⏭⏯◀⏪⏮🔼⏫🔽⏬⏸-⏺⏏🎦🔅🔆📶🛜📳📴♀♂⚧✖➕-➗🟰♾‼⁉❓-❕❗〰💱💲⚕♻⚜🔱📛🔰⭕✅☑✔❌❎➰➿〽✳✴❇©®™🔟-🔤🅰🆎🅱🆑-🆓ℹ🆔Ⓜ🆕🆖🅾🆗🅿🆘-🆚🈁🈂🈷🈶🈯🉐🈹🈚🈲🉑🈸🈴🈳㊗㊙🈺🈵🔴🟠-🟢🔵🟣🟤⚫⚪🟥🟧-🟩🟦🟪🟫⬛⬜◼◻◾◽▪▫🔶-🔻💠🔘🔳🔲🏁🚩🎌🏴🏳🇦-🇿\uD83E\uDD89\uD83E\uDD8F\uD83E\uDDBE\uD83E\uDDC6\uD83E\uddcd\uD83E\udddf\uD83E\ude99]|#️⃣|\\*️⃣|0️⃣|1️⃣|2️⃣|3️⃣|4️⃣|5️⃣|6️⃣|7️⃣|8️⃣|9️⃣)+");
  private final float originalFontSize;

  public AutoScaledEmojiTextView(Context context) {
    this(context, null);
  }

  public AutoScaledEmojiTextView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public AutoScaledEmojiTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    TypedArray typedArray = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.textSize});
    originalFontSize = typedArray.getDimensionPixelSize(0, 0);
    typedArray.recycle();
  }

  @Override
  public void setText(@Nullable CharSequence text, BufferType type) {
    float scale = text != null ? getTextScale(text.toString()) : 1;
    super.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalFontSize * scale);
    super.setText(text, type);
  }

    
  private float getTextScale(String text) {
    if (text.length() > 21 || text.isEmpty() || Character.isLetter(text.charAt(0))) {
      return 1;
    }
    int emojiCount = countGraphemes(text, 8);
    if (emojiCount > 8) {
      return 1;
    }

    boolean isEmoji = emojiRegex.matcher(text).matches();
    if (isEmoji) {
      float scale = 1.25f;
      if (emojiCount <= 6) scale += 0.25f;
      if (emojiCount <= 4) scale += 0.25f;
      if (emojiCount <= 2) scale += 0.25f;
      return scale;
    }
    return 1;
  }

  public static int countGraphemes(String text, int max) {
    // Create a BreakIterator for grapheme boundaries
    BreakIterator graphemeIterator = BreakIterator.getCharacterInstance(Locale.getDefault());

    // Set the text to be analyzed
    graphemeIterator.setText(text);

    // Initialize count
    int graphemeCount = 0;

    // Iterate over the text and count graphemes
    int start = graphemeIterator.first();
    for (int end = graphemeIterator.next(); end != BreakIterator.DONE; start = end, end = graphemeIterator.next()) {
      if (++graphemeCount > max) break;
    }

    return graphemeCount;
  }
}
