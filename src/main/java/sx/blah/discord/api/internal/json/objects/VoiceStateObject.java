package sx.blah.discord.api.internal.json.objects;

import com.austinv11.etf.util.GetterMethod;
import com.austinv11.etf.util.SetterMethod;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import sx.blah.discord.util.ID;

/**
 * Represents a json voice state object.
 */
public class VoiceStateObject {
	/**
	 * The guild id of the voice state.
	 */
	private ID guild_id;
	/**
	 * The voice channel id of the voice state.
	 */
	private ID channel_id;
	/**
	 * The user id with the state.
	 */
	private ID user_id;
	/**
	 * The session id of the voice state.
	 */
	public String session_id;
	/**
	 * Whether the user is deafened.
	 */
	public boolean deaf;
	/**
	 * Whether the user is muted.
	 */
	public boolean mute;
	/**
	 * Whether the user has deafened themselves.
	 */
	public boolean self_deaf;
	/**
	 * Whether the user has muted themselves.
	 */
	public boolean self_mute;
	/**
	 * Whether user is suppressed.
	 */
	public boolean suppress;
	
	@JsonGetter("guild_id")
	public String getStringGuildID() {
		return guild_id.getStringID();
	}
	
	@JsonSetter("guild_id")
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
	
	@JsonGetter("user_id")
	public String getStringUserID() {
		return user_id.getStringID();
	}
	
	@JsonSetter("user_id")
	public void setStringUserID(String id) {
		this.user_id = new ID(id);
	}
	
	@GetterMethod("user_id")
	public long getLongUserID() {
		return user_id.getLongID();
	}
	
	@SetterMethod("user_id")
	public void setLongUserID(long id) {
		this.user_id = new ID(id);
	}
}
