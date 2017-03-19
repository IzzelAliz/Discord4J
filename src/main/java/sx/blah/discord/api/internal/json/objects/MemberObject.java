package sx.blah.discord.api.internal.json.objects;

import com.austinv11.etf.util.GetterMethod;
import com.austinv11.etf.util.SetterMethod;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import sx.blah.discord.util.ID;

import java.util.Arrays;

/**
 * Represents a json member object.
 */
public class MemberObject {
	/**
	 * The user associated with this member.
	 */
	public UserObject user;
	/**
	 * The nickname of the member.
	 */
	public String nick;
	/**
	 * The roles of the member.
	 */
	private ID[] roles;
	/**
	 * When the member joined the guild.
	 */
	public String joined_at;
	/**
	 * Whether this member is deafened.
	 */
	public boolean deaf;
	/**
	 * Whether this member is muted.
	 */
	public boolean mute;

	public MemberObject() {}

	public MemberObject(UserObject user, long[] roles) {
		this.user = user;
		this.roles = ID.of(roles);
	}
	
	@JsonGetter("roles")
	public String[] getStringRoles() {
		return ID.toStrings(roles);
	}
	
	@JsonSetter("roles")
	public void setStringRoles(String[] ids) {
		this.roles = ID.of(ids);
	}
	
	@GetterMethod("roles")
	public long[] getRoles() {
		return Arrays.stream(roles).mapToLong(ID::getLongID).toArray();
	}
	
	@SetterMethod("roles")
	public void setRoles(RoleObject[] ids) {
		this.roles = Arrays.stream(ids).mapToLong(RoleObject::getLongID).mapToObj(ID::new).toArray(ID[]::new);
	}
}
