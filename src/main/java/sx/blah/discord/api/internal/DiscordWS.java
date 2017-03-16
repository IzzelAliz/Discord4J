package sx.blah.discord.api.internal;

import com.austinv11.etf.common.TermTypes;
import com.austinv11.etf.erlang.ErlangMap;
import com.austinv11.etf.parsing.ETFParser;
import com.austinv11.etf.writing.ETFWriter;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.IShard;
import sx.blah.discord.api.internal.etf.GatewayPayload;
import sx.blah.discord.api.internal.etf.IdentifyRequest;
import sx.blah.discord.api.internal.etf.ResumeRequest;
import sx.blah.discord.handle.impl.events.DisconnectedEvent;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.LogMarkers;

import java.net.URI;
import java.nio.channels.UnresolvedAddressException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class DiscordWS extends WebSocketAdapter {

	State state;
	private WebSocketClient wsClient;

	DiscordClientImpl client;
	ShardImpl shard;
	private String gateway;

	long seq = 0;
	String sessionId;

	private DispatchHandler dispatchHandler;
	HeartbeatHandler heartbeatHandler;

	/**
	 * When the bot has received all available guilds.
	 */
	public boolean isReady = false;

	/**
	 * When the bot has received the initial Ready payload from Discord.
	 */
	public boolean hasReceivedReady = false;

	DiscordWS(IShard shard, String gateway, int maxMissedPings) {
		this.client = (DiscordClientImpl) shard.getClient();
		this.shard = (ShardImpl) shard;
		this.gateway = gateway;
		this.dispatchHandler = new DispatchHandler(this, this.shard);
		this.heartbeatHandler = new HeartbeatHandler(this, maxMissedPings);
		this.state = State.CONNECTING;
	}

	@Override
	public void onWebSocketText(String message) {
		throw new DiscordException("Discord sent us a string, please report this!");
	}

	@Override
	public void onWebSocketConnect(Session sess) {
		Discord4J.LOGGER.info(LogMarkers.WEBSOCKET, "Websocket Connected.");
		super.onWebSocketConnect(sess);
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		super.onWebSocketClose(statusCode, reason);
		Discord4J.LOGGER.info(LogMarkers.WEBSOCKET, "Shard {} websocket disconnected with status code {} and reason \"{}\".", shard.getInfo()[0], statusCode, reason);

		isReady = false;
		hasReceivedReady = false;
		heartbeatHandler.shutdown();
		if (!(this.state == State.DISCONNECTING || statusCode == 4003 || statusCode == 4004 || statusCode == 4005 ||
				statusCode == 4010) && !(statusCode == 1001 && reason != null && reason.equals("Shutdown"))) {
			this.state = State.RESUMING;
			client.getDispatcher().dispatch(new DisconnectedEvent(DisconnectedEvent.Reason.ABNORMAL_CLOSE, shard));
			client.reconnectManager.scheduleReconnect(this);
		}
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		super.onWebSocketError(cause);
		if (cause instanceof UnresolvedAddressException) {
			Discord4J.LOGGER.warn(LogMarkers.WEBSOCKET, "Caught UnresolvedAddressException. Internet outage?");
		} else {
			Discord4J.LOGGER.error(LogMarkers.WEBSOCKET, "Encountered websocket error: ", cause);
		}

		if (this.state == State.RESUMING) {
			client.reconnectManager.onReconnectError();
		}
	}
	
	//TODO remove
	private void printToUnsigned(byte[] data) {
		int[] unsignedStuff = new int[data.length];
		for (int i = 0; i < data.length; i++)
			unsignedStuff[i] = Byte.toUnsignedInt(data[i]);
		System.out.println(Arrays.toString(unsignedStuff));
	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		try {
			payload = Arrays.copyOfRange(payload, offset, offset+len-1);
			printToUnsigned(payload);
			ETFParser parser = (payload[1] == TermTypes.MAP_EXT ? DiscordUtils.PARTIAL_ETF_CONFIG : DiscordUtils.FULL_ETF_CONFIG).createParser(payload);
			payload = parser.getRawData();
			printToUnsigned(payload);
			
			while (parser.peek() != TermTypes.MAP_EXT)
				parser.next();
			
			ErlangMap map = parser.nextMap();
			
			if (Discord4J.LOGGER.isTraceEnabled(LogMarkers.WEBSOCKET_TRAFFIC)) {
				Discord4J.LOGGER.trace(LogMarkers.WEBSOCKET_TRAFFIC, "Received: " + map.toString());
			}
			
			GatewayOps op = GatewayOps.get(map.getInt("op"));
			ErlangMap d = map.containsKey("d") ? map.getErlangMap("d") : null;
			
			if (map.containsKey("s") && map.get("s") != null) {
				seq = map.getInt("s");
			}
			
			switch (op) {
				case HELLO:
					if (Discord4J.LOGGER.isTraceEnabled(LogMarkers.WEBSOCKET))
						Discord4J.LOGGER.trace(LogMarkers.WEBSOCKET, "Shard {} _trace: {}", shard.getShardNumber(), d.getErlangList("_trace"));
					
					heartbeatHandler.begin(d.getInt("heartbeat_interval"));
					if (this.state != State.RESUMING) {
						send(GatewayOps.IDENTIFY, new IdentifyRequest(client.getToken(), new int[]{shard.getShardNumber(), shard.getTotalShardCount()}));
					} else {
						client.reconnectManager.onReconnectSuccess();
						send(GatewayOps.RESUME, new ResumeRequest(client.getToken(), sessionId, seq));
					}
					break;
				case RECONNECT:
					this.state = State.RESUMING;
					client.getDispatcher().dispatch(new DisconnectedEvent(DisconnectedEvent.Reason.RECONNECT_OP, shard));
					heartbeatHandler.shutdown();
					send(GatewayOps.RESUME, new ResumeRequest(client.getToken(), sessionId, seq));
					break;
				case DISPATCH:
					try {
						dispatchHandler.handle(payload);
					} catch (Exception e) {
						Discord4J.LOGGER.error(LogMarkers.WEBSOCKET, "Discord4J Internal Exception", e);
					}
					break;
				case INVALID_SESSION:
					this.state = State.RECONNECTING;
					client.getDispatcher().dispatch(new DisconnectedEvent(DisconnectedEvent.Reason.INVALID_SESSION_OP, shard));
					invalidate();
					send(GatewayOps.IDENTIFY, new IdentifyRequest(client.getToken(), new int[]{shard.getShardNumber(), shard.getTotalShardCount()}));
					break;
				case HEARTBEAT:
					send(GatewayOps.HEARTBEAT, seq);
				case HEARTBEAT_ACK:
					heartbeatHandler.ack();
					break;
				case UNKNOWN:
					Discord4J.LOGGER.debug(LogMarkers.WEBSOCKET, "Received unknown opcode, {}", map.getInt("op"));
					break;
			}
		} catch (Exception e) {
			Discord4J.LOGGER.error(LogMarkers.WEBSOCKET, "ETF Parsing exception!", e);
		}
	}

	void connect() {
		WebSocketClient previous = wsClient; // for cleanup
		try {
			wsClient = new WebSocketClient(new SslContextFactory());
			wsClient.setDaemon(true);
			wsClient.getPolicy().setMaxBinaryMessageSize(Integer.MAX_VALUE);
			wsClient.getPolicy().setMaxTextMessageSize(Integer.MAX_VALUE);
			wsClient.start();
			wsClient.connect(this, new URI(gateway), new ClientUpgradeRequest());
		} catch (Exception e) {
			Discord4J.LOGGER.error(LogMarkers.WEBSOCKET, "Encountered error while connecting websocket: ", e);
		} finally {
			if (previous != null) {
				CompletableFuture.runAsync(() -> {
					try {
						previous.stop();
					} catch (Exception e) {
						Discord4J.LOGGER.error(LogMarkers.WEBSOCKET, "Error while stopping previous websocket: ", e);
					}
				});
			}
		}
	}

	void shutdown() {
		Discord4J.LOGGER.debug(LogMarkers.WEBSOCKET, "Shard {} shutting down.", shard.getInfo()[0]);
		this.state = State.DISCONNECTING;

		try {
			heartbeatHandler.shutdown();
			getSession().close(1000, null); // Discord doesn't care about the reason
			wsClient.stop();
			hasReceivedReady = false;
			isReady = false;
		} catch (Exception e) {
			Discord4J.LOGGER.error(LogMarkers.WEBSOCKET, "Error while shutting down websocket: ", e);
		}
	}

	private void invalidate() {
		this.isReady = false;
		this.hasReceivedReady = false;
		this.seq = 0;
		this.sessionId = null;
		this.shard.guildList.clear();
		this.shard.privateChannels.clear();
	}

	public void send(GatewayOps op, Object payload) {
		send(new GatewayPayload(op, payload));
	}

	public void send(GatewayPayload payload) {
		send(DiscordUtils.PARTIAL_ETF_CONFIG.createWriter().writeMap(payload));
	}

	public void send(ETFWriter message) {
//		String filteredMessage = message.replace(client.getToken(), "hunter2");
		if (getSession() != null && getSession().isOpen()) {
			if (Discord4J.LOGGER.isTraceEnabled(LogMarkers.WEBSOCKET_TRAFFIC))
				Discord4J.LOGGER.trace(LogMarkers.WEBSOCKET_TRAFFIC, "Sending: " + DiscordUtils.PARTIAL_ETF_CONFIG.createParser(message.toBytes()).nextMap().toString());
			
			printToUnsigned(message.toBytes());
			getSession().getRemote().sendBytesByFuture(message.toBuffer());
		} else {
			Discord4J.LOGGER.warn(LogMarkers.WEBSOCKET, "Attempt to send message on closed session: {}", message.toString());
		}
	}

	enum State {
		IDLE,
		CONNECTING,
		READY,
		RECONNECTING,
		RESUMING,
		DISCONNECTING
	}
}
