package sx.blah.discord.api.internal.etf.event;

import sx.blah.discord.api.internal.json.objects.UserObject;

/**
 * This event is received when a member is updated in a guild.
 */
public class GuildMemberUpdateEventResponse {

	/**
	 * The guild affected.
	 */
	public long guild_id;

	/**
	 * The user's roles.
	 */
	public long[] roles;

	/**
	 * The user.
	 */
	public UserObject user;

	/**
	 * The user's new nick.
	 */
	public String nick;
}
