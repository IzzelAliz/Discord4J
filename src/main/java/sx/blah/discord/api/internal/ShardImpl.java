package sx.blah.discord.api.internal;

import com.koloboke.collect.map.hash.HashLongObjMap;
import com.koloboke.collect.map.hash.HashLongObjMaps;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.IShard;
import sx.blah.discord.api.internal.etf.PresenceUpdateRequest;
import sx.blah.discord.api.internal.json.objects.PrivateChannelObject;
import sx.blah.discord.api.internal.json.requests.PrivateChannelCreateRequest;
import sx.blah.discord.handle.impl.events.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.PresenceUpdateEvent;
import sx.blah.discord.handle.impl.obj.PresenceImpl;
import sx.blah.discord.handle.impl.obj.User;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.LogMarkers;
import sx.blah.discord.util.RateLimitException;
import sx.blah.discord.util.RequestBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ShardImpl implements IShard {

	public volatile DiscordWS ws;

	private final String gateway;
	private final int shardNumber, totalShards;

	private final DiscordClientImpl client;
	final HashLongObjMap<IGuild> guildList = HashLongObjMaps.newMutableMap();
	final HashLongObjMap<IPrivateChannel> privateChannels = HashLongObjMaps.newMutableMap();

	ShardImpl(IDiscordClient client, String gateway, int shardNumber, int totalShards) {
		this.client = (DiscordClientImpl) client;
		this.gateway = gateway;
		this.shardNumber = shardNumber;
		this.totalShards = totalShards;
	}

	@Override
	public IDiscordClient getClient() {
		return this.client;
	}

	@Override
	public int[] getInfo() {
		return new int[] {shardNumber, totalShards};
	}
	
	@Override
	public int getShardNumber() {
		return shardNumber;
	}
	
	@Override
	public int getTotalShardCount() {
		return totalShards;
	}
	
	@Override
	public void login() throws DiscordException {
		Discord4J.LOGGER.trace(LogMarkers.API, "Shard logging in.");
		this.ws = new DiscordWS(this, gateway, client.maxMissedPings);
		this.ws.connect();
	}

	@Override
	public void logout() throws DiscordException {
		checkLoggedIn("logout");

		Discord4J.LOGGER.info(LogMarkers.API, "Shard {} logging out.", getInfo()[0]);
		getConnectedVoiceChannels().forEach(channel -> {
			RequestBuffer.RequestFuture<IVoiceChannel> request = RequestBuffer.request(() -> {
				channel.leave();
				return channel;
			});
			request.get();
		});
		getClient().getDispatcher().dispatch(new DisconnectedEvent(DisconnectedEvent.Reason.LOGGED_OUT, this));
		ws.shutdown();
	}

	@Override
	public boolean isReady() {
		return ws != null && ws.isReady;
	}

	@Override
	public boolean isLoggedIn() {
		return ws != null && ws.hasReceivedReady;
	}

	@Override
	public long getResponseTime() {
		return ws.heartbeatHandler.getAckResponseTime();
	}

	@Override
	public void changePlayingText(String playingText) {
		updatePresence(getClient().getOurUser().getPresence().getStatus(), playingText,
				getClient().getOurUser().getPresence().getStreamingUrl().orElse(null));
	}

	@Override
	public void online(String playingText) {
		updatePresence(StatusType.ONLINE, playingText);
	}

	@Override
	public void online() {
		online(getClient().getOurUser().getPresence().getPlayingText().orElse(null));
	}

	@Override
	public void idle(String playingText) {
		updatePresence(StatusType.IDLE, playingText);
	}

	@Override
	public void idle() {
		idle(getClient().getOurUser().getPresence().getPlayingText().orElse(null));
	}

	@Override
	public void streaming(String playingText, String streamingUrl) {
		updatePresence(StatusType.STREAMING, playingText, streamingUrl);
	}

	@Override
	@Deprecated
	public void changeStatus(Status status) {
		// old functionality just in case
		if (status.getType() == Status.StatusType.STREAM) {
			streaming(status.getStatusMessage(), status.getUrl().orElse(null));
		} else {
			changePlayingText(status.getStatusMessage());
		}
	}

	@Override
	@Deprecated
	public void changePresence(boolean isIdle) {
		// old functionality just in case
		if (isIdle)
			idle();
		else
			online();
	}

	private void updatePresence(StatusType status, String playing) {
		updatePresence(status, playing, null);
	}

	private void updatePresence(StatusType status, String playing, String streamUrl) {
		final boolean isIdle = status == StatusType.IDLE; // temporary until v6
		IUser ourUser = getClient().getOurUser();

		IPresence oldPresence = ourUser.getPresence();
		IPresence newPresence = new PresenceImpl(Optional.ofNullable(playing), Optional.ofNullable(streamUrl), status);

		if (!newPresence.equals(oldPresence)) {
			((User) ourUser).setPresence(newPresence);
			getClient().getDispatcher().dispatch(new PresenceUpdateEvent(ourUser, oldPresence, newPresence));
		}

		ws.send(GatewayOps.STATUS_UPDATE,
				new PresenceUpdateRequest(isIdle ? System.currentTimeMillis() : null, ourUser.getPresence()));
	}

	@Override
	public List<IChannel> getChannels(boolean priv) {
		List<IChannel> channels;
		synchronized (guildList) {
			 channels = guildList.values()
					.stream()
					.map(IGuild::getChannels)
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}
		if (priv) {
			synchronized (privateChannels) {
				channels.addAll(privateChannels.values());
			}
		}
		return channels;
	}

	@Override
	public List<IChannel> getChannels() {
		return getChannels(false);
	}

	@Override
	public IChannel getChannelByID(String id) {
		return getChannels(true).stream()
				.filter(c -> c.getStringID().equals(id))
				.findAny().orElse(null);
	}
	
	@Override
	public IChannel getChannelByID(long id) {
		return getChannels(true).stream()
				.filter(c -> c.getLongID() == id)
				.findAny().orElse(null);
	}

	@Override
	public List<IVoiceChannel> getVoiceChannels() {
		synchronized (guildList) {
			return guildList.values()
					.stream()
					.map(IGuild::getVoiceChannels)
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}
	}

	@Override
	public List<IVoiceChannel> getConnectedVoiceChannels() {
		return getClient()
				.getOurUser()
				.getConnectedVoiceChannels()
				.stream()
				.filter(vc -> vc.getShard()
						.equals(this))
				.collect(Collectors.toList());
	}

	@Override
	public IVoiceChannel getVoiceChannelByID(String id) {
		return getVoiceChannels().stream()
				.filter(c -> c.getStringID().equalsIgnoreCase(id))
				.findAny().orElse(null);
	}
	
	@Override
	public IVoiceChannel getVoiceChannelByID(long id) {
		return getVoiceChannels().stream()
				.filter(c -> c.getLongID() == id)
				.findAny().orElse(null);
	}

	@Override
	public List<IGuild> getGuilds() {
		synchronized (guildList) {
			return new ArrayList<>(guildList.values());
		}
	}

	@Override
	public IGuild getGuildByID(String guildID) {
		synchronized (guildList) {
			return guildList.get(Long.parseUnsignedLong(guildID));
		}
	}
	
	@Override
	public IGuild getGuildByID(long guildID) {
		synchronized (guildList) {
			return guildList.get(guildID);
		}
	}
	
	@Override
	public List<IUser> getUsers() {
		synchronized (guildList) {
			return guildList.values()
					.stream()
					.map(IGuild::getUsers)
					.flatMap(List::stream)
					.distinct()
					.collect(Collectors.toList());
		}
	}

	@Override
	public IUser getUserByID(String userID) {
		IUser ourUser = getClient().getOurUser(); // List of users doesn't include the bot user. Check if the id is that of the bot.
		if (ourUser != null && ourUser.getID().equals(userID))
			return ourUser;
		
		IGuild guild;
		synchronized (guildList) {
			guild = guildList.values()
					.stream()
					.filter(g -> g.getUserByID(userID) != null)
					.findFirst()
					.orElse(null);
		}
		
		return guild != null ? guild.getUserByID(userID) : null;
	}
	
	@Override
	public IUser getUserByID(long userID) {
		IUser ourUser = getClient().getOurUser(); // List of users doesn't include the bot user. Check if the id is that of the bot.
		if (ourUser != null && ourUser.getLongID() == userID)
			return ourUser;
		
		IGuild guild;
		synchronized (guildList) {
			guild = guildList.values()
					.stream()
					.filter(g -> g.getUserByID(userID) != null)
					.findFirst()
					.orElse(null);
		}
		
		return guild != null ? guild.getUserByID(userID) : null;
	}
	
	@Override
	public List<IRole> getRoles() {
		synchronized (guildList) {
			return guildList.values()
					.stream()
					.map(IGuild::getRoles)
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}
	}

	@Override
	public IRole getRoleByID(String roleID) {
		return getRoles().stream()
				.filter(r -> r.getID().equalsIgnoreCase(roleID))
				.findAny().orElse(null);
	}
	
	@Override
	public IRole getRoleByID(long roleID) {
		return getRoles().stream()
				.filter(r -> r.getLongID() == roleID)
				.findAny().orElse(null);
	}
	
	@Override
	public List<IMessage> getMessages(boolean includePrivate) {
		return getChannels(includePrivate).stream()
				.map(IChannel::getMessageHistory)
				.flatMap(List::stream)
				.collect(Collectors.toList());
	}

	@Override
	public List<IMessage> getMessages() {
		return getMessages(false);
	}

	@Override
	public IMessage getMessageByID(String messageID) {
		synchronized (guildList) {
			for (IGuild guild : guildList.values()) {
				IMessage message = guild.getMessageByID(messageID);
				if (message != null)
					return message;
			}
		}

		synchronized (privateChannels) {
			for (IPrivateChannel privateChannel : privateChannels.values()) {
				IMessage message = privateChannel.getMessageByID(messageID);
				if (message != null)
					return message;
			}
		}

		return null;
	}
	
	@Override
	public IMessage getMessageByID(long messageID) {
		synchronized (guildList) {
			for (IGuild guild : guildList.values()) {
				IMessage message = guild.getMessageByID(messageID);
				if (message != null)
					return message;
			}
		}
		
		synchronized (privateChannels) {
			for (IPrivateChannel privateChannel : privateChannels.values()) {
				IMessage message = privateChannel.getMessageByID(messageID);
				if (message != null)
					return message;
			}
		}
		
		return null;
	}
	
	@Override
	public IPrivateChannel getOrCreatePMChannel(IUser user) throws DiscordException, RateLimitException {
		checkReady("get PM channel");

		if (user.equals(getClient().getOurUser()))
			throw new DiscordException("Cannot PM yourself!");

		synchronized (privateChannels) {
			Optional<IPrivateChannel> opt = privateChannels.values()
					.stream()
					.filter(c -> c.getRecipient().getLongID() == user.getLongID())
					.findAny();
			if (opt.isPresent())
				return opt.get();
		}

		PrivateChannelObject pmChannel = client.REQUESTS.POST.makeRequest(
				DiscordEndpoints.USERS+getClient().getOurUser().getStringID()+"/channels",
				new PrivateChannelCreateRequest(user.getLongID()),
				PrivateChannelObject.class);
		IPrivateChannel channel = DiscordUtils.getPrivateChannelFromJSON(this, pmChannel);
		synchronized (privateChannels) {
			privateChannels.put(channel.getLongID(), channel);
		}
		return channel;
	}
}
