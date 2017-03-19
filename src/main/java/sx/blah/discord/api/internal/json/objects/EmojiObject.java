package sx.blah.discord.api.internal.json.objects;

import com.austinv11.etf.util.GetterMethod;
import com.austinv11.etf.util.SetterMethod;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import sx.blah.discord.util.ID;

/**
 * Represents a json custom emoji object.
 */
public class EmojiObject {
	/**
	 * The id of the emoji.
	 */
	private ID id;
	/**
	 * The name of the emoji.
	 */
	public String name;
	/**
	 * Whether the emoji must be wrapped in colons.
	 */
	public boolean require_colons;
	/**
	 * Whether the emoji is managed.
	 */
	public boolean managed;
	
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
