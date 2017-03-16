package sx.blah.discord.api.internal.etf.event;

import sx.blah.discord.api.internal.json.objects.ReactionEmojiObject;

/**
 * Response when reactions are added or removed on a message.
 */
public class ReactionEventResponse {
	/**
	 * The id of the user who reacted
	 */
	public long user_id;
	/**
	 * The id of the message
	 */
	public long message_id;
	/**
	 * The emoji involved
	 */
	public ReactionEmojiObject emoji;
	/**
	 * The channel involved
	 */
	public long channel_id;
}
