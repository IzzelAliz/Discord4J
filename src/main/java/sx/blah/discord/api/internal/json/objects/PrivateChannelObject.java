package sx.blah.discord.api.internal.json.objects;

import com.austinv11.etf.util.GetterMethod;
import com.austinv11.etf.util.SetterMethod;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import sx.blah.discord.util.ID;

/**
 * Represents a json private channel object.
 * Used for convenience.
 */
public class PrivateChannelObject {
	/**
	 * The id of the last message sent in the channel.
	 */
	private ID last_message_id;
	/**
	 * the recipient of the channel.
	 */
	public UserObject recipient;
	/**
	 * The id of the channel.
	 */
	private ID id;
	/**
	 * Whether the channel is private.
	 */
	public boolean is_private;
	
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
}
