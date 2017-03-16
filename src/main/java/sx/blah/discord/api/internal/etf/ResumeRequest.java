package sx.blah.discord.api.internal.etf;

public class ResumeRequest {

	/**
	 * The session token
	 */
	public String token;

	/**
	 * The session id to resume.
	 */
	public String session_id;

	/**
	 * This is the last cached value of {@link GatewayPayloads}
	 */
	public long seq;

	public ResumeRequest(String token, String session_id, long seq) {
		this.token = token;
		this.session_id = session_id;
		this.seq = seq;
	}
}
