package sx.blah.discord.api.internal.json.requests;


/**
 * This is used to request a private channel be created with a user
 */
public class PrivateChannelCreateRequest {

	/**
	 * The user id of the user the channel directed towards
	 */
	public String recipient_id;

	public PrivateChannelCreateRequest(long recipient_id) {
		this.recipient_id = Long.toUnsignedString(recipient_id);
	}
}
