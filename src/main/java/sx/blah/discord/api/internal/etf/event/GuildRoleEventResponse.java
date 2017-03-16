package sx.blah.discord.api.internal.etf.event;

import sx.blah.discord.api.internal.json.objects.RoleObject;

/**
 * This is received when a role is created or updated in a guild.
 */
public class GuildRoleEventResponse {

	/**
	 * The role involved.
	 */
	public RoleObject role;

	/**
	 * The guild id of the guild involved.
	 */
	public long guild_id;
}
