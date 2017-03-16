package sx.blah.discord.api.internal.json.objects;

import com.austinv11.etf.util.GetterMethod;
import com.austinv11.etf.util.SetterMethod;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import sx.blah.discord.util.ID;

/**
 * Represents a json guild object.
 */
public class GuildObject {
	/**
	 * The id of the guild.
	 */
	private ID id;
	/**
	 * The name of the guild.
	 */
	public String name;
	/**
	 * The icon of the guild.
	 */
	public String icon;
	/**
	 * The id of the user that owns the guild.
	 */
	private ID owner_id;
	/**
	 * The region the guild's voice server is in.
	 */
	public String region;
	/**
	 * The id of the afk voice channel.
	 */
	private ID afk_channel_id;
	/**
	 * The timeout for moving people to the afk voice channel.
	 */
	public int afk_timeout;
	/**
	 * Whether this guild is embeddable via a widget.
	 */
	public boolean embed_enabled;
	/**
	 * The id of the embedded channel.
	 */
	private ID embed_channel_id;
	/**
	 * Level of verification.
	 */
	public int verification_level;
	/**
	 * Default message notifications level.
	 */
	public int default_messages_notifications;
	/**
	 * Array of role objects
	 */
	public RoleObject[] roles;
	/**
	 * Array of emoji objects.
	 */
	public EmojiObject[] emojis;
	/**
	 * Array of guild features.
	 */
	public String[] features;
	/**
	 * Required MFA level for the guild.
	 */
	public int mfa_level;
	/**
	 * The date the self user joined the guild.
	 */
	public String joined_at;
	/**
	 * Whether the guild is considered to be large.
	 */
	public boolean large;
	/**
	 * Whether the guild is unavailable.
	 */
	public boolean unavailable;
	/**
	 * Number of members in the guild.
	 */
	public int member_count;
	/**
	 * Array of voice states for the members in the guild.
	 */
	public VoiceStateObject[] voice_states;
	/**
	 * Array of members.
	 */
	public MemberObject[] members;
	/**
	 * Array of channels.
	 */
	public ChannelObject[] channels;
	/**
	 * Array of presences for members in the guild.
	 */
	public PresenceObject[] presences;
	
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
	
	@JsonGetter("owner_id")
	public String getStringOwnerID() {
		return owner_id.getStringID();
	}
	
	@JsonSetter("owner_id")
	public void setStringOwnerID(String id) {
		this.owner_id = new ID(id);
	}
	
	@GetterMethod("owner_id")
	public long getLongOwnerID() {
		return owner_id.getLongID();
	}
	
	@SetterMethod("owner_id")
	public void setLongOwnerID(long id) {
		this.owner_id = new ID(id);
	}
	
	@JsonGetter("afk_channel_id")
	public String getStringAFKChannelID() {
		return afk_channel_id.getStringID();
	}
	
	@JsonSetter("afk_channel_id")
	public void setStringAFKChannelID(String id) {
		this.afk_channel_id = new ID(id);
	}
	
	@GetterMethod("afk_channel_id")
	public long getLongAFKChannelID() {
		return afk_channel_id.getLongID();
	}
	
	@SetterMethod("afk_channel_id")
	public void setLongAFKChannelID(long id) {
		this.afk_channel_id = new ID(id);
	}
	
	@JsonGetter("embed_channel_id")
	public String getStringEmbedChannelID() {
		return embed_channel_id.getStringID();
	}
	
	@JsonSetter("embed_channel_id")
	public void setStringEmbedChannelID(String id) {
		this.embed_channel_id = new ID(id);
	}
	
	@GetterMethod("embed_channel_id")
	public long getLongEmbedChannelID() {
		return embed_channel_id.getLongID();
	}
	
	@SetterMethod("embed_channel_id")
	public void setLongEmbedChannelID(long id) {
		this.embed_channel_id = new ID(id);
	}
}
