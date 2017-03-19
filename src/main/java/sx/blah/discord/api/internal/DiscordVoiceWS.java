package sx.blah.discord.api.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.internal.etf.GatewayPayload;
import sx.blah.discord.api.internal.etf.voice.SelectProtocolRequest;
import sx.blah.discord.api.internal.etf.voice.VoiceIdentifyRequest;
import sx.blah.discord.api.internal.etf.voice.VoiceSpeakingRequest;
import sx.blah.discord.api.internal.etf.voice.VoiceDescriptionResponse;
import sx.blah.discord.api.internal.etf.voice.VoiceReadyResponse;
import sx.blah.discord.api.internal.etf.voice.VoiceSpeakingResponse;
import sx.blah.discord.api.internal.etf.voice.VoiceUpdateResponse;
import sx.blah.discord.handle.audio.impl.AudioManager;
import sx.blah.discord.handle.impl.events.VoiceDisconnectedEvent;
import sx.blah.discord.handle.impl.events.VoiceUserSpeakingEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.LogMarkers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DiscordVoiceWS extends WebSocketAdapter {

	private WebSocketClient wsClient;

	private DiscordClientImpl client;
	private ShardImpl shard;
	private IGuild guild;
	private int ssrc;
	private DatagramSocket udpSocket;
	private InetSocketAddress address;
	private String endpoint;
	private String token;
	private byte[] secret;
	private boolean isSpeaking = false;

	private ScheduledExecutorService keepAlive = Executors.newSingleThreadScheduledExecutor(DiscordUtils.createDaemonThreadFactory("Voice Keep-Alive Handler"));
	private ScheduledExecutorService sendHandler = Executors.newSingleThreadScheduledExecutor(DiscordUtils.createDaemonThreadFactory("Voice Send Handler"));

	public DiscordVoiceWS(VoiceUpdateResponse response, ShardImpl shard) {
		this.shard = shard;
		this.client = (DiscordClientImpl) shard.getClient();
		this.token = response.token;
		this.endpoint = response.endpoint;
		this.guild = client.getGuildByID(response.guild_id);

		try {
			wsClient = new WebSocketClient(new SslContextFactory());
			wsClient.setDaemon(true);
			wsClient.start();
			wsClient.connect(this, new URI("wss://" + response.endpoint), new ClientUpgradeRequest());
		} catch (Exception e) {
			Discord4J.LOGGER.error(LogMarkers.VOICE_WEBSOCKET, "Encountered error while initializing voice websocket: {}", e);
		}
	}

	@Override
	public void onWebSocketConnect(Session sess) {
		super.onWebSocketConnect(sess);
		Discord4J.LOGGER.info(LogMarkers.VOICE_WEBSOCKET, "Voice websocket connected.");

		VoiceIdentifyRequest request = new VoiceIdentifyRequest(guild.getLongID(), client.getOurUser().getLongID(), shard.ws.sessionId, token);
		send(VoiceOps.IDENTIFY, request);
	}

	@Override
	public void onWebSocketText(String message) {
		try {
			JsonNode json = DiscordUtils.MAPPER.readTree(message);
			VoiceOps op = VoiceOps.get(json.get("op").asInt());
			JsonNode d = json.has("d") && !json.get("d").isNull() ? json.get("d") : null;

			switch (op) {
				case READY:
					VoiceReadyResponse ready = DiscordUtils.MAPPER.treeToValue(d, VoiceReadyResponse.class);
					this.ssrc = ready.ssrc;

					try {
						udpSocket = new DatagramSocket();
						address = new InetSocketAddress(endpoint, ready.port);
						Pair<String, Integer> ourIP = doIPDiscovery();

						SelectProtocolRequest request = new SelectProtocolRequest(ourIP.getLeft(), ourIP.getRight());
						send(VoiceOps.SELECT_PAYLOAD, request);

						beginHeartbeat(ready.heartbeat_interval);
					} catch (IOException e) {
						Discord4J.LOGGER.error(LogMarkers.VOICE_WEBSOCKET, "Discord4J Internal Exception", e);
					}
					break;
				case SESSION_DESCRIPTION:
					VoiceDescriptionResponse description = DiscordUtils.MAPPER.treeToValue(d, VoiceDescriptionResponse.class);
					this.secret = description.secret_key;

					setupSendThread();
					break;
				case SPEAKING:
					VoiceSpeakingResponse speaking = DiscordUtils.MAPPER.treeToValue(d, VoiceSpeakingResponse.class);
					IUser user = client.getUserByID(speaking.user_id);
					client.dispatcher.dispatch(new VoiceUserSpeakingEvent(guild.getConnectedVoiceChannel(), user, speaking.ssrc, speaking.isSpeaking));
					break;
				case UNKNOWN:
					Discord4J.LOGGER.debug(LogMarkers.VOICE_WEBSOCKET, "Received unknown voice opcode, {}", message);
					break;
			}
		} catch (IOException e) {
			Discord4J.LOGGER.error(LogMarkers.WEBSOCKET, "JSON Parsing exception!", e);
		}
	}

	private void beginHeartbeat(int interval) {
		keepAlive.scheduleAtFixedRate(() -> {
			send(VoiceOps.HEARTBEAT, System.currentTimeMillis());
		}, 0, interval, TimeUnit.MILLISECONDS);
	}

	public void disconnect(VoiceDisconnectedEvent.Reason reason) {
		try {
			client.dispatcher.dispatch(new VoiceDisconnectedEvent(guild, reason));
			synchronized (client.voiceConnections) {
				client.voiceConnections.remove(guild.getLongID());
			}
			keepAlive.shutdownNow();
			sendHandler.shutdownNow();
			udpSocket.close();
			if (getSession() != null) getSession().close(1000, null); // Discord doesn't care about the reason
			wsClient.stop();
		} catch (Exception e) {
			if (!(e instanceof InterruptedException)) Discord4J.LOGGER.error(LogMarkers.VOICE_WEBSOCKET, "Error while shutting down voice websocket: ", e);
		}
	}

	private void setupSendThread() {
		Runnable sendThread = new Runnable() {
			char seq = 0;
			int timestamp = 0;      //Used to sync up our packets within the same timeframe of other people talking.

			@Override
			public void run() {
				try {
					if (!udpSocket.isClosed()) {
						byte[] data = guild.getAudioManager().getAudio();
						if (data != null && data.length > 0 && !Discord4J.audioDisabled.get()) {
							AudioPacket packet = new AudioPacket(seq, timestamp, ssrc, data, secret);
							if (!isSpeaking) setSpeaking(true);
							udpSocket.send(packet.asUdpPacket(address));

							if (seq + 1 > Character.MAX_VALUE) {
								seq = 0;
							} else {
								seq++;
							}

							timestamp += AudioManager.OPUS_FRAME_SIZE;
						} else if (isSpeaking) {
							setSpeaking(false);
						}
					}
				} catch (Exception e) {
					Discord4J.LOGGER.error(LogMarkers.VOICE_WEBSOCKET, "Discord Internal Exception", e);
				}
			}
		};
		sendHandler.scheduleWithFixedDelay(sendThread, 0, AudioManager.OPUS_FRAME_TIME_AMOUNT - 1, TimeUnit.MILLISECONDS);
	}

	private void setSpeaking(boolean isSpeaking) {
		this.isSpeaking = isSpeaking;
		send(VoiceOps.SPEAKING, new VoiceSpeakingRequest(isSpeaking));
	}

	private Pair<String, Integer> doIPDiscovery() throws IOException {
		byte[] data = ByteBuffer.allocate(70).putInt(ssrc).array();
		DatagramPacket discoveryPacket = new DatagramPacket(data, data.length, address);
		udpSocket.send(discoveryPacket);

		DatagramPacket responsePacket = new DatagramPacket(new byte[70], 70);
		udpSocket.receive(responsePacket);

		byte[] receivedData = responsePacket.getData();
		String ip = new String(Arrays.copyOfRange(receivedData, 4, 68)).trim();
		int port = ((((int) receivedData[69]) & 0x000000FF) << 8) | (((int) receivedData[68]) & 0x000000FF);

		return Pair.of(ip, port);
	}

	private void send(VoiceOps op, Object payload) {
		send(new GatewayPayload(op, payload));
	}

	private void send(GatewayPayload payload) {
		try {
			send(DiscordUtils.MAPPER_NO_NULLS.writeValueAsString(payload));
		} catch (JsonProcessingException e) {
			Discord4J.LOGGER.error(LogMarkers.VOICE_WEBSOCKET, "JSON Parsing exception!", e);
		}
	}

	private void send(String message) {
		if (getSession() != null && getSession().isOpen()) {
			getSession().getRemote().sendStringByFuture(message);
		} else {
			Discord4J.LOGGER.warn(LogMarkers.VOICE_WEBSOCKET, "Attempt to send message on closed session: {}", message);
		}
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		super.onWebSocketError(cause);
		Discord4J.LOGGER.error(LogMarkers.VOICE_WEBSOCKET, "Encountered error on voice websocket: ", cause);
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		super.onWebSocketClose(statusCode, reason);
		Discord4J.LOGGER.info(LogMarkers.VOICE_WEBSOCKET, "Voice Websocket disconnected with status code {} and reason \"{}\".", statusCode, reason);
		disconnect(VoiceDisconnectedEvent.Reason.ABNORMAL_CLOSE);
	}
}
