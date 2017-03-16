package sx.blah.discord.api.internal.etf.event;

import sx.blah.discord.api.internal.json.objects.UserObject;

/**
 * This response is received when a user leaves a guild
 */
public class GuildMemberRemoveEventResponse {

	/**
	 * The user who left
	 */
	public UserObject user;

	/**
	 * The guild the user left
	 */
	public long guild_id;
}
