package sx.blah.discord.api.internal.json.objects;

import com.austinv11.etf.util.GetterMethod;
import com.austinv11.etf.util.SetterMethod;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import sx.blah.discord.util.ID;

/**
 * Represents a json user object.
 */
public class UserObject {
	/**
	 * The username of the user.
	 */
	public String username;
	/**
	 * The discriminator of the user.
	 */
	public String discriminator;
	/**
	 * The id of the user.
	 */
	private ID id;
	/**
	 * The avatar of the user.
	 */
	public String avatar;
	/**
	 * Whether the user is a bot.
	 */
	public boolean bot = false;
	
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
