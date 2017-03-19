package sx.blah.discord.api.internal.etf.event;

import sx.blah.discord.api.internal.json.objects.PrivateChannelObject;
import sx.blah.discord.api.internal.json.objects.UnavailableGuildObject;
import sx.blah.discord.api.internal.json.objects.UserObject;

public class ReadyResponse {
	public int v;
	public UserObject user;
//	public ErlangList shard; FIXME why doesn't this work?
	public String session_id;
	public PrivateChannelObject[] private_channels;
	public UnavailableGuildObject[] guilds;
	public String[] _trace;
}
