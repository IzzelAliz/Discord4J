package sx.blah.discord.api.internal.json.objects;

import com.austinv11.etf.util.GetterMethod;
import com.austinv11.etf.util.SetterMethod;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import sx.blah.discord.util.ID;

/**
 * Represents a json webhook object.
 */
public class WebhookObject {
	/**
	 * The id of the webhook.
	 */
	private ID id;
	/**
	 * The id of the guild this webhook is in.
	 */
	private ID guild_id;
	/**
	 * The id of the channel this webhook can post to.
	 */
	private ID channel_id;
	/**
	 * The user that will post with this webhook.
	 */
	public UserObject user;
	/**
	 * The name of the webhook.
	 */
	public String name;
	/**
	 * The avatar of the webhook.
	 */
	public String avatar;
	/**
	 * The token of the webhook.
	 */
	public String token;
	
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
}
