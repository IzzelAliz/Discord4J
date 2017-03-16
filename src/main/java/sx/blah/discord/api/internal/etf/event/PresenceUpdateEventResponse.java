package sx.blah.discord.api.internal.etf.event;


import sx.blah.discord.api.internal.json.objects.GameObject;
import sx.blah.discord.api.internal.json.objects.UserObject;

/**
 * This is received when a user's presence changes
 */
public class PresenceUpdateEventResponse {

	/**
	 * The user this represents
	 */
	public UserObject user;

	/**
	 * The status for the user, either: "idle" or "online"
	 */
	public String status;

	/**
	 * The game the user is playing (or null if no game being played)
	 */
	public GameObject game;

	/**
	 * The roles the user is a part of
	 */
	public long[] roles;

	/**
	 * The guild the presence updated in
	 */
	public long guild_id;
}
