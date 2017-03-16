package sx.blah.discord.api.internal.json.objects;

import com.austinv11.etf.util.GetterMethod;
import com.austinv11.etf.util.SetterMethod;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import sx.blah.discord.util.ID;

/**
 * Represents a json presence object.
 */
public class PresenceObject {
	/**
	 * The user associated with this presence.
	 */
	public UserObject user;
	/**
	 * The status of the presence.
	 */
	public String status;
	/**
	 * The roles the user has.
	 */
	public RoleObject[] roles;
	/**
	 * The nickname of the user.
	 */
	public String nick;
	/**
	 * The guild id of the presence.
	 */
	private ID guild_id;
	/**
	 * The game of the presence.
	 */
	public GameObject game;
	
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
}
