package sx.blah.discord.api.internal.json.objects;

import com.austinv11.etf.util.GetterMethod;
import com.austinv11.etf.util.SetterMethod;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import sx.blah.discord.util.ID;

/**
 * Represents a json permission overwrite object.
 */
public class OverwriteObject {
	/**
	 * The id of the overwrite.
	 */
	private ID id;
	/**
	 * The type of the overwrite.
	 */
	public String type;
	/**
	 * The permissions allowed by this overwrite.
	 */
	public int allow;
	/**
	 * The permissions denied by this overwrite.
	 */
	public int deny;

	public OverwriteObject() {}

	public OverwriteObject(String type, String id, int allow, int deny) {
		this.id = new ID(id);
		this.type = type;
		this.allow = allow;
		this.deny = deny;
	}
	
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
