package sx.blah.discord.api.internal.etf;

import sx.blah.discord.api.internal.GatewayOps;
import sx.blah.discord.api.internal.VoiceOps;

public class GatewayPayload {
	public String t;
	public Integer s;
	public Integer op;
	public Object d;

	public GatewayPayload() {}

	public GatewayPayload(GatewayOps op, Object request) {
		this(null, null, op.ordinal(), request);
	}

	public GatewayPayload(VoiceOps op, Object request) {
		this(null, null, op.ordinal(), request);
	}

	private GatewayPayload(String t, Integer s, Integer op, Object d) {
		this.t = t;
		this.s = s;
		this.op = op;
		this.d = d;
	}
}
