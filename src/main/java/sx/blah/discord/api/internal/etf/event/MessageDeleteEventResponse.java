package sx.blah.discord.api.internal.etf.event;

/**
 * This response is received when a message is deleted
 */
public class MessageDeleteEventResponse {

	/**
	 * The message id
	 */
	public long id;

	/**
	 * The channel the message was deleted from
	 */
	public long channel_id;

	public MessageDeleteEventResponse() {}

	public MessageDeleteEventResponse(long id, long channel_id) {
		this.id = id;
		this.channel_id = channel_id;
	}
}
