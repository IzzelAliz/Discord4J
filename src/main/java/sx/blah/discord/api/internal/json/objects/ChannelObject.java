package sx.blah.discord.api.internal.json.objects;

import com.austinv11.etf.util.GetterMethod;
import com.austinv11.etf.util.SetterMethod;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import sx.blah.discord.util.ID;

/**
 * Represents a json channel object.
 */
public class ChannelObject {
	/**
	 * The id of the channel.
	 */
	private ID id;
	/**
	 * The id of the guild this channel is in.
	 */
	private ID guild_id;
	/**
	 * The name of the channel.
	 */
	public String name;
	/**
	 * The type of the channel.
	 */
	public String type;
	/**
	 * The position of the channel.
	 */
	public int position;
	/**
	 * Whether the channel is private or not.
	 */
	public boolean is_private;
	/**
	 * Array of permission overwrites.
	 */
	public OverwriteObject[] permission_overwrites;
	/**
	 * Topic of the channel.
	 */
	public String topic;
	/**
	 * ID of the last message sent in the channel.
	 */
	private ID last_message_id;
	/**
	 * When the last pin was made in the channel.
	 */
	public String last_pin_timestamp;
	/**
	 * Bitrate of the channel if it is voice type.
	 */
	public int bitrate;
	/**
	 * Maximum number of users allowed in the channel if it is voice type.
	 */
	public int user_limit;
	/**
	 * Recipients of the channel if it is private type.
	 */
	public UserObject[] recipients;
	
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
	
	@JsonGetter("last_message_id")
	public String getStringLastMessageID() {
		return last_message_id.getStringID();
	}
	
	@JsonSetter("last_message_id")
	public void setStringLastMessageID(String id) {
		this.last_message_id = new ID(id);
	}
	
	@GetterMethod("last_message_id")
	public long getLongLastMessageID() {
		return last_message_id.getLongID();
	}
	
	@SetterMethod("last_message_id")
	public void setLongLastMessageID(long id) {
		this.last_message_id = new ID(id);
	}
	
	@JsonGetter("guild_id")
	public String getStringGuildID() {
		return guild_id.getStringID();
	}
	
	@JsonGetter("guild_id")
	public void setStringGuildID(String id) {
		this.guild_id = new ID(id);
	}
	
	@GetterMethod("guild_id")
	public long getLongGuildID() {
		return guild_id.getLongID();
	}
	
	@SetterMethod("guild_id")
	public void setLongGuildID(long id) {
		this.guild_id = new ID(id);
	}
}
