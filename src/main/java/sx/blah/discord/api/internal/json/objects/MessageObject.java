package sx.blah.discord.api.internal.json.objects;

import com.austinv11.etf.util.GetterMethod;
import com.austinv11.etf.util.SetterMethod;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import sx.blah.discord.util.ID;

/**
 * Represents a message json object.
 */
public class MessageObject {
	/**
	 * The id of the message.
	 */
	private ID id;
	/**
	 * The type of the message.
	 */
	public int type;
	/**
	 * The channel id for the channel this message was sent in.
	 */
	private ID channel_id;
	/**
	 * The author of the message.
	 */
	public UserObject author;
	/**
	 * The content of the message.
	 */
	public String content;
	/**
	 * The timestamp of when the message was sent.
	 */
	public String timestamp;
	/**
	 * The timestamp of when the message was last edited.
	 */
	public String edited_timestamp;
	/**
	 * Whether the message should be read with tts.
	 */
	public boolean tts;
	/**
	 * Whether the message mentions everyone.
	 */
	public boolean mention_everyone;
	/**
	 * The users the message mentions.
	 */
	public UserObject[] mentions;
	/**
	 * The roles the message mentions.
	 */
	private ID[] mention_roles;
	/**
	 * The attachments on the message.
	 */
	public AttachmentObject[] attachments;
	/**
	 * The embeds in the message.
	 */
	public EmbedObject[] embeds;
	/**
	 * The nonce of the message.
	 */
	public String nonce;
	/**
	 * Whether the message is pinned.
	 */
	public Boolean pinned;
	/**
	 * The reactions on the message.
	 */
	public ReactionObject[] reactions;
	/**
	 * The id of the webhook that sent the message.
	 */
	private ID webhook_id;
	
	@JsonGetter("id")
	public String getStringID() {
		return id.getStringID();
	}
	
	@JsonSetter("id")
	public void setStringID(String id) {
		this.id = new ID(id);
	}
	
	@GetterMethod("id")
	public long getLongID() {
		return id.getLongID();
	}
	
	@SetterMethod("id")
	public void setLongID(long id) {
		this.id = new ID(id);
	}
	
	@JsonGetter("channel_id")
	public String getStringChannelID() {
		return channel_id.getStringID();
	}
	
	@JsonSetter("channel_id")
	public void setStringChannelID(String id) {
		this.channel_id = new ID(id);
	}
	
	@GetterMethod("channel_id")
	public long getLongChannelID() {
		return channel_id.getLongID();
	}
	
	@SetterMethod("channel_id")
	public void setLongChannelID(long id) {
		this.channel_id = new ID(id);
	}
	
	@JsonGetter("mention_roles")
	public String[] getStringRoles() {
		return ID.toStrings(mention_roles);
	}
	
	@JsonSetter("mention_roles")
	public void setStringRoles(String[] ids) {
		this.mention_roles = ID.of(ids);
	}
	
	@GetterMethod("mention_roles")
	public long[] getLongRoles() {
		return ID.toLongs(mention_roles);
	}
	
	@SetterMethod("mention_roles")
	public void setLongRoles(long[] ids) {
		this.mention_roles = ID.of(ids);
	}
	
	@JsonGetter("webhook_id")
	public String getStringWebhookID() {
		return webhook_id.getStringID();
	}
	
	@JsonSetter("webhook_id")
	public void setStringWebhookID(String id) {
		this.webhook_id = new ID(id);
	}
	
	@GetterMethod("webhook_id")
	public long getLongWebhookID() {
		return webhook_id.getLongID();
	}
	
	@SetterMethod("webhook_id")
	public void setLongWebhookID(long id) {
		this.webhook_id = new ID(id);
	}

	/**
	 * Represents a json message attachment object.
	 */
	public static class AttachmentObject {
		/**
		 * The id of the attachment.
		 */
		private ID id;
		/**
		 * The name of the attached file.
		 */
		public String filename;
		/**
		 * The size of the attached file.
		 */
		public int size;
		/**
		 * The url of the attached file.
		 */
		public String url;
		/**
		 * The proxy url of the attached file.
		 */
		public String proxy_url;
		/**
		 * The height of the attached file if it's an image.
		 */
		public int height;
		/**
		 * The width of the attached file if it's an image.
		 */
		public int width;
		
		@JsonGetter("id")
		public String getStringID() {
			return id.getStringID();
		}
		
		@JsonSetter("id")
		public void setStringID(String id) {
			this.id = new ID(id);
		}
		
		@GetterMethod("id")
		public long getLongID() {
			return id.getLongID();
		}
		
		@SetterMethod("id")
		public void setLongID(long id) {
			this.id = new ID(id);
		}
	}

	/**
	 * Represents a json reaction object.
	 */
	public static class ReactionObject {
		/**
		 * The number of this reaction on the message.
		 */
		public int count;
		/**
		 * Whether the self user has reacted with this reaction.
		 */
		public boolean me;
		/**
		 * The reaction emoji.
		 */
		public ReactionEmojiObject emoji;
	}
}
