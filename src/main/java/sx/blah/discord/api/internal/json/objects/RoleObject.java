package sx.blah.discord.api.internal.json.objects;

import com.austinv11.etf.util.GetterMethod;
import com.austinv11.etf.util.SetterMethod;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import sx.blah.discord.util.ID;

/**
 * Represents a json role object.
 */
public class RoleObject {
	/**
	 * The id of the role.
	 */
	private ID id;
	/**
	 * The name of the role.
	 */
	public String name;
	/**
	 * The color of the role.
	 */
	public int color;
	/**
	 * Whether the role should be displayed separately in the online users list.
	 */
	public boolean hoist;
	/**
	 * The position of the role.
	 */
	public int position;
	/**
	 * The permissions granted by this role.
	 */
	public int permissions;
	/**
	 * Whether the role is managed.
	 */
	public boolean managed;
	/**
	 * Whether the role is mentionable.
	 */
	public boolean mentionable;
	
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
