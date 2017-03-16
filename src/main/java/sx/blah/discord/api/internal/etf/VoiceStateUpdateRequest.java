package sx.blah.discord.api.internal.etf;

public class VoiceStateUpdateRequest {

	public long guild_id;
	public Long channel_id;
	public boolean self_mute;
	public boolean self_deaf;

	public VoiceStateUpdateRequest(long guild_id, Long channel_id, boolean self_mute, boolean self_deaf) {
		this.guild_id = guild_id;
		this.channel_id = channel_id;
		this.self_mute = self_mute;
		this.self_deaf = self_deaf;
	}
}
