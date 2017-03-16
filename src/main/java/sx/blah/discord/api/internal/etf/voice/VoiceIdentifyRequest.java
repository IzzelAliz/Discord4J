package sx.blah.discord.api.internal.etf.voice;

public class VoiceIdentifyRequest {
	private long server_id;
	private long user_id;
	private String session_id;
	private String token;

	public VoiceIdentifyRequest(long server_id, long user_id, String session_id, String token) {
		this.server_id = server_id;
		this.user_id = user_id;
		this.session_id = session_id;
		this.token = token;
	}
}
