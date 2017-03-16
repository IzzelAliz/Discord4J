package sx.blah.discord.api.internal.etf.event;

/**
 * This is received when a bot bulk deletes
 */
public class MessageDeleteBulkEventResponse {

	/**
	 * The ids of the messages deleted.
	 */
	public long[] ids;

	/**
	 * The id of the channel the messages belong to.
	 */
	public long channel_id;
}
