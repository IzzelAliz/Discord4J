package sx.blah.discord.handle.impl.obj;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.IShard;
import sx.blah.discord.api.internal.DiscordClientImpl;
import sx.blah.discord.api.internal.DiscordEndpoints;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.api.internal.json.objects.RoleObject;
import sx.blah.discord.api.internal.json.requests.RoleEditRequest;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import java.awt.*;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;

public class Role implements IRole {

	/**
	 * Where the role should be displayed. -1 is @everyone, it is always last
	 */
	protected volatile int position;

	/**
	 * The permissions the role has.
	 */
	protected volatile EnumSet<Permissions> permissions;

	/**
	 * The role name
	 */
	protected volatile String name;

	/**
	 * Whether this role is managed via plugins like twitch
	 */
	protected volatile boolean managed;

	/**
	 * The role id
	 */
	protected volatile long id;

	/**
	 * Whether to display this role separately from others
	 */
	protected volatile boolean hoist;

	/**
	 * The DECIMAL format for the color
	 */
	protected volatile Color color;

	/**
	 * Whether you can @mention this role.
	 */
	protected volatile boolean mentionable;

	/**
	 * The guild this role belongs to
	 */
	protected volatile IGuild guild;

	public Role(int position, int permissions, String name, boolean managed, long id, boolean hoist, int color, boolean mentionable, IGuild guild) {
		this.position = position;
		this.permissions = Permissions.getAllowedPermissionsForNumber(permissions);
		this.name = name;
		this.managed = managed;
		this.id = id;
		this.hoist = hoist;
		this.color = new Color(color);
		this.mentionable = mentionable;
		this.guild = guild;
	}

	@Override
	public int getPosition() {
		getGuild().getRoles().sort((r1, r2) -> {
			int originalPos1 = ((Role) r1).position;
			int originalPos2 = ((Role) r2).position;
			if (originalPos1 == originalPos2) {
				return r2.getCreationDate().compareTo(r1.getCreationDate());
			} else {
				return originalPos1 - originalPos2;
			}
		});
		return getGuild().getRoles().indexOf(this);
	}

	/**
	 * Sets the CACHED role position.
	 *
	 * @param position The role position.
	 */
	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public EnumSet<Permissions> getPermissions() {
		return permissions.clone();
	}

	/**
	 * Sets the CACHED enabled permissions.
	 *
	 * @param permissions The permissions number.
	 */
	public void setPermissions(int permissions) {
		this.permissions = Permissions.getAllowedPermissionsForNumber(permissions);
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the CACHED role name.
	 *
	 * @param name The name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean isManaged() {
		return managed;
	}

	@Override
	public boolean isHoisted() {
		return hoist;
	}

	/**
	 * Sets whether this role is hoisted in the CACHE.
	 *
	 * @param hoist True if hoisted, false if otherwise.
	 */
	public void setHoist(boolean hoist) {
		this.hoist = hoist;
	}

	@Override
	public Color getColor() {
		return color;
	}

	/**
	 * Sets the CACHED role color.
	 *
	 * @param color The color decimal number.
	 */
	public void setColor(int color) {
		this.color = new Color(color);
	}

	@Override
	public boolean isMentionable() {
		return mentionable || isEveryoneRole();
	}

	/**
	 * Sets whether this role is mentionable in the CACHE.
	 *
	 * @param mentionable True if mentionable, false if otherwise.
	 */
	public void setMentionable(boolean mentionable) {
		this.mentionable = mentionable;
	}

	@Override
	public IGuild getGuild() {
		return guild;
	}

	@Override
	public void edit(Color color, boolean hoist, String name, EnumSet<Permissions> permissions, boolean isMentionable) throws DiscordException, RateLimitException, MissingPermissionsException {
		DiscordUtils.checkPermissions(getClient(), guild, Collections.singletonList(this), EnumSet.of(Permissions.MANAGE_ROLES));

		if (color == null)
			throw new IllegalArgumentException("Color must not be null.");
		if (name == null || name.length() < 1 || name.length() > 32)
			throw new IllegalArgumentException("Role name must be between 1 and 32 characters!");
		if (permissions == null)
			throw new IllegalArgumentException("Permissions set must not be null.");

		DiscordUtils.getRoleFromJSON(getGuild(), ((DiscordClientImpl) getClient()).REQUESTS.PATCH.makeRequest(
				DiscordEndpoints.GUILDS + guild.getStringID() + "/roles/" + getStringID(),
				new RoleEditRequest(color, hoist, name, permissions, isMentionable),
				RoleObject.class));
	}

	@Override
	public void changeColor(Color color) throws DiscordException, RateLimitException, MissingPermissionsException {
		edit(color, isHoisted(), getName(), getPermissions(), isMentionable());
	}

	@Override
	public void changeHoist(boolean hoist) throws DiscordException, RateLimitException, MissingPermissionsException {
		edit(getColor(), hoist, getName(), getPermissions(), isMentionable());
	}

	@Override
	public void changeName(String name) throws DiscordException, RateLimitException, MissingPermissionsException {
		edit(getColor(), isHoisted(), name, getPermissions(), isMentionable());
	}

	@Override
	public void changePermissions(EnumSet<Permissions> permissions) throws DiscordException, RateLimitException, MissingPermissionsException {
		edit(getColor(), isHoisted(), getName(), permissions, isMentionable());
	}

	@Override
	public void changeMentionable(boolean mentionable) throws DiscordException, RateLimitException, MissingPermissionsException {
		edit(getColor(), isHoisted(), getName(), getPermissions(), mentionable);
	}

	@Override
	public void delete() throws DiscordException, RateLimitException, MissingPermissionsException {
		DiscordUtils.checkPermissions(((Guild) guild).client, guild, Collections.singletonList(this), EnumSet.of(Permissions.MANAGE_ROLES));

		((DiscordClientImpl) getClient()).REQUESTS.DELETE.makeRequest(DiscordEndpoints.GUILDS+guild.getStringID()+"/roles/"+getStringID());
	}

	@Override
	public IRole copy() {
		return new Role(position, Permissions.generatePermissionsNumber(permissions), name, managed, id, hoist,
				color.getRGB(), mentionable, guild);
	}
	
	@Override
	public long getLongID() {
		return id;
	}
	
	@Override
	public IDiscordClient getClient() {
		return getGuild().getClient();
	}

	@Override
	public IShard getShard() {
		return getGuild().getShard();
	}

	@Override
	public boolean isEveryoneRole() {
		return guild.getEveryoneRole().equals(this);
	}

	@Override
	public boolean isDeleted() {
		return getGuild().getRoleByID(id) != this;
	}

	@Override
	public String mention() {
		return isMentionable() ? (isEveryoneRole() ? "@everyone" : "<@&"+id+">") : name;
	}

	@Override
	public String toString() {
		return mention();
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;

		return this.getClass().isAssignableFrom(other.getClass()) && ((IRole) other).getID().equals(getID());
	}
}
