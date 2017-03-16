package sx.blah.discord.api.internal.etf.event;

import com.austinv11.etf.erlang.ErlangList;
import sx.blah.discord.api.internal.json.objects.PrivateChannelObject;
import sx.blah.discord.api.internal.json.objects.UnavailableGuildObject;
import sx.blah.discord.api.internal.json.objects.UserObject;

public class ReadyResponse {
	public int v;
	public UserObject user;
	public ErlangList shard;
	public String session_id;
	public PrivateChannelObject[] private_channels;
	public UnavailableGuildObject[] guilds;
	public String[] _trace;
}
