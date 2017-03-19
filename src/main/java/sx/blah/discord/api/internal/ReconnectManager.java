package sx.blah.discord.api.internal;

import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.collect.set.hash.HashObjSets;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.ReconnectFailureEvent;
import sx.blah.discord.handle.impl.events.ReconnectSuccessEvent;
import sx.blah.discord.util.LogMarkers;

import java.util.concurrent.ThreadLocalRandom;

public class ReconnectManager {

	private final IDiscordClient client;
	private final HashObjSet<DiscordWS> toReconnect = HashObjSets.newMutableSet();

	private final int maxAttempts;
	private volatile int curAttempt = 0;

	ReconnectManager(IDiscordClient client, int maxAttempts) {
		this.client = client;
		this.maxAttempts = maxAttempts;
	}

	synchronized void scheduleReconnect(DiscordWS ws) {
		Discord4J.LOGGER.trace(LogMarkers.WEBSOCKET, "Reconnect scheduled for shard {}.", ws.shard.getInfo()[0]);
		toReconnect.add(ws);
		if (toReconnect.size() == 1) { // If this is the only WS in the queue, immediately begin the reconnect process
			beginReconnect();
		}
	}

	synchronized void onReconnectSuccess() {
		DiscordWS reconnected = toReconnect.cursor().elem();
		toReconnect.remove(reconnected);
		Discord4J.LOGGER.info(LogMarkers.WEBSOCKET, "Reconnect for shard {} succeeded.", reconnected.shard.getInfo()[0]);
		client.getDispatcher().dispatch(new ReconnectSuccessEvent(reconnected.shard));
		curAttempt = 0;
		if (!toReconnect.isEmpty()) {
			try {
				Thread.sleep(5000); // Login ratelimit
				beginReconnect(); // Start next reconnect
			} catch (Exception e) {
				Discord4J.LOGGER.error(LogMarkers.WEBSOCKET, "Discord4J Internal Exception", e);
			}
		}
	}

	synchronized void onReconnectError() {
		DiscordWS reconnected = toReconnect.cursor().elem();
		client.getDispatcher().dispatch(new ReconnectFailureEvent(reconnected.shard, curAttempt, maxAttempts));
		if (curAttempt <= maxAttempts) {
			try {
				Thread.sleep(getReconnectDelay()); // Sleep for back off
				curAttempt++;
				doReconnect(); // Attempt again
			} catch (Exception e) {
				Discord4J.LOGGER.error(LogMarkers.WEBSOCKET, "Discord4J Internal Exception", e);
			}
		} else {
			Discord4J.LOGGER.info(LogMarkers.WEBSOCKET, "Reconnect for shard {} failed after {} attempts.", reconnected.shard.getShardNumber(), maxAttempts);
			curAttempt = 0; // Reset curAttempt for next ws
			toReconnect.remove(reconnected); // Remove the current ws from the queue. We've given up trying to reconnect it
			if (!toReconnect.isEmpty())  {
				beginReconnect(); // Start process for next in queue
			}
		}
	}

	private synchronized void beginReconnect() {
		Discord4J.LOGGER.info(LogMarkers.WEBSOCKET, "Beginning reconnect for shard {}.", toReconnect.cursor().elem().shard.getShardNumber());
		doReconnect(); // Perform reconnect
	}

	private synchronized void doReconnect() {
		Discord4J.LOGGER.info(LogMarkers.WEBSOCKET, "Performing reconnect attempt {}.", curAttempt);
		toReconnect.cursor().elem().connect();
	}

	private synchronized long getReconnectDelay() {
		return ((2 * curAttempt) + ThreadLocalRandom.current().nextLong(0, 2)) * 1000;
	}
}
