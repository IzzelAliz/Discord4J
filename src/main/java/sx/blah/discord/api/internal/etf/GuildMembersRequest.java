package sx.blah.discord.api.internal.etf;

public class GuildMembersRequest {
	/**
	 * The guild's id
	 */
	public long guild_id;

	/**
	 * String the username starts with or empty for all users.
	 */
	public String query = "";

	/**
	 * The limit on users to receive or 0 for max.
	 */
	public int limit = 0;

	public GuildMembersRequest(long guild_id) {
		this.guild_id = guild_id;
	}
}
