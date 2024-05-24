/* Autogenerated file, do not edit manually */
package chat.delta.rpc.types;

public class Message {
  public Integer chatId;
  public Integer dimensionsHeight;
  public Integer dimensionsWidth;
  public DownloadState downloadState;
  public Integer duration;
  /* An error text, if there is one. */
  public String error;
  public String file;
  public Integer fileBytes;
  public String fileMime;
  public String fileName;
  public Integer fromId;
  public Boolean hasDeviatingTimestamp;
  public Boolean hasHtml;
  /* Check if a message has a POI location bound to it. These locations are also returned by `get_locations` method. The UI may decide to display a special icon beside such messages. */
  public Boolean hasLocation;
  public Integer id;
  /* True if the message was sent by a bot. */
  public Boolean isBot;
  public Boolean isForwarded;
  public Boolean isInfo;
  public Boolean isSetupmessage;
  public String overrideSenderName;
  public Integer parentId;
  public MessageQuote quote;
  public Reactions reactions;
  public Integer receivedTimestamp;
  public Contact sender;
  public String setupCodeBegin;
  public Boolean showPadlock;
  public Integer sortTimestamp;
  public Integer state;
  public String subject;
  /* when is_info is true this describes what type of system message it is */
  public SystemMessageType systemMessageType;
  public String text;
  public Integer timestamp;
  public VcardContact vcardContact;
  public Integer videochatType;
  public String videochatUrl;
  public Viewtype viewType;
  public WebxdcMessageInfo webxdcInfo;
}