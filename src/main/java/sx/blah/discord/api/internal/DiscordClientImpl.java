package sx.blah.discord.api.internal;

import com.koloboke.collect.map.hash.HashLongObjMap;
import com.koloboke.collect.map.hash.HashLongObjMaps;
import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.collect.set.hash.HashObjSets;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.IShard;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.api.internal.json.objects.InviteObject;
import sx.blah.discord.api.internal.json.objects.UserObject;
import sx.blah.discord.api.internal.json.objects.VoiceRegionObject;
import sx.blah.discord.api.internal.json.requests.AccountInfoChangeRequest;
import sx.blah.discord.api.internal.json.responses.ApplicationInfoResponse;
import sx.blah.discord.api.internal.json.responses.GatewayResponse;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.ShardReadyEvent;
import sx.blah.discord.handle.impl.obj.User;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.modules.ModuleLoader;
import sx.blah.discord.util.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Defines the client. This class receives and sends messages, as well as holds our user data.
 */
public final class DiscordClientImpl implements IDiscordClient {

	static {
		ServiceUtil.loadServices();
	}

	/**
	 * The shards this client controls.
	 */
	private final HashObjSet<IShard> shards = HashObjSets.newMutableSet();

	/**
	 * User we are logged in as
	 */
	volatile User ourUser;

	/**
	 * Our token, so we can send XHR to Discord.
	 */
	protected volatile String token;

	/**
	 * Event dispatcher.
	 */
	final EventDispatcher dispatcher;

	/**
	 * Reconnect manager.
	 */
	final ReconnectManager reconnectManager;

	/**
	 * The module loader for this client.
	 */
	private final ModuleLoader loader;

	/**
	 * Caches the available regions for discord.
	 */
	private final HashObjSet<IRegion> REGIONS = HashObjSets.newMutableSet();

	/**
	 * Holds the active connections to voice sockets.
	 */
	public final HashLongObjMap<DiscordVoiceWS> voiceConnections = HashLongObjMaps.newMutableMap();

	/**
	 * The maximum amount of pings discord can miss before a new session is created.
	 */
	final int maxMissedPings;

	/**
	 * Whether the websocket should act as a daemon.
	 */
	private final boolean isDaemon;

	/**
	 * The total number of shards this client manages.
	 */
	private final int shardCount;

	/**
	 * The requests holder object.
	 */
	public final Requests REQUESTS = new Requests(this);

	/**
	 * Timer to keep the program alive if the client is not daemon
	 */
	volatile Timer keepAlive;
	private final int retryCount;
	private final int maxCacheCount;

	public DiscordClientImpl(String token, int shardCount, boolean isDaemon, int maxMissedPings, int maxReconnectAttempts,
							 int retryCount, int maxCacheCount) {
		this.token = "Bot " + token;
		this.retryCount = retryCount;
		this.maxMissedPings = maxMissedPings;
		this.isDaemon = isDaemon;
		this.shardCount = shardCount;
		this.maxCacheCount = maxCacheCount;
		this.dispatcher = new EventDispatcher(this);
		this.reconnectManager = new ReconnectManager(this, maxReconnectAttempts);
		this.loader = new ModuleLoader(this);

		final DiscordClientImpl instance = this;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (instance.keepAlive != null)
				instance.keepAlive.cancel();
		}));
	}

	@Override
	public List<IShard> getShards() {
		synchronized (shards) {
			return new ArrayList<>(this.shards);
		}
	}

	@Override
	public int getShardCount() {
		return this.shardCount;
	}

	@Override
	public EventDispatcher getDispatcher() {
		return this.dispatcher;
	}

	@Override
	public ModuleLoader getModuleLoader() {
		return this.loader;
	}

	@Override
	public String getToken() {
		return this.token;
	}

	private void changeAccountInfo(String username, String avatar) throws DiscordException, RateLimitException {
		checkLoggedIn("change account info");

		Discord4J.LOGGER.debug(LogMarkers.API, "Changing account info.");
		REQUESTS.PATCH.makeRequest(DiscordEndpoints.USERS+"@me", new AccountInfoChangeRequest(username, avatar));
	}

	@Override
	public void changeUsername(String username) throws DiscordException, RateLimitException {
		changeAccountInfo(username, Image.forUser(ourUser).getData());
	}

	@Override
	public void changeAvatar(Image avatar) throws DiscordException, RateLimitException {
		changeAccountInfo(ourUser.getName(), avatar.getData());
	}

	@Override
	public IUser getOurUser() {
		return ourUser;
	}

	@Override
	public List<IRegion> getRegions() throws DiscordException, RateLimitException {
		synchronized (REGIONS) {
			if (REGIONS.isEmpty()) {
				VoiceRegionObject[] regions = REQUESTS.GET.makeRequest(
						DiscordEndpoints.VOICE+"regions", VoiceRegionObject[].class);
				
				Arrays.stream(regions)
						.map(DiscordUtils::getRegionFromJSON)
						.forEach(REGIONS::add);
			}
			
			return new ArrayList<>(REGIONS);
		}
	}

	@Override
	public IRegion getRegionByID(String regionID) {
		try {
			return getRegions().stream()
					.filter(r -> r.getID().equals(regionID))
					.findAny().orElse(null);
		} catch (RateLimitException | DiscordException e) {
			Discord4J.LOGGER.error(LogMarkers.API, "Discord4J Internal Exception", e);
		}
		return null;
	}

	private ApplicationInfoResponse getApplicationInfo() throws DiscordException, RateLimitException {
		return REQUESTS.GET.makeRequest(DiscordEndpoints.APPLICATIONS+"/@me", ApplicationInfoResponse.class);
	}

	@Override
	public String getApplicationDescription() throws DiscordException {
		try {
			return getApplicationInfo().description;
		} catch (RateLimitException e) {
			Discord4J.LOGGER.error(LogMarkers.API, "Discord4J Internal Exception", e);
		}
		return null;
	}

	@Override
	public String getApplicationIconURL() throws DiscordException {
		try {
			ApplicationInfoResponse info = getApplicationInfo();
			return String.format(DiscordEndpoints.APPLICATION_ICON, info.id, info.icon);
		} catch (RateLimitException e) {
			Discord4J.LOGGER.error(LogMarkers.API, "Discord4J Internal Exception", e);
		}
		return null;
	}

	@Override
	public String getApplicationClientID() throws DiscordException {
		try {
			return getApplicationInfo().id;
		} catch (RateLimitException e) {
			Discord4J.LOGGER.error(LogMarkers.API, "Discord4J Internal Exception", e);
		}
		return null;
	}
	
	@Override
	public long getApplicationClientLongID() throws DiscordException {
		try {
			return Long.parseUnsignedLong(getApplicationInfo().id);
		} catch (RateLimitException e) {
			Discord4J.LOGGER.error(LogMarkers.API, "Discord4J Internal Exception", e);
		}
		return 0;
	}

	@Override
	public String getApplicationName() throws DiscordException {
		try {
			return getApplicationInfo().name;
		} catch (RateLimitException e) {
			Discord4J.LOGGER.error(LogMarkers.API, "Discord4J Internal Exception", e);
		}
		return null;
	}

	@Override
	public IUser getApplicationOwner() throws DiscordException {
		try {
			UserObject owner = getApplicationInfo().owner;

			IUser user = getUserByID(owner.getLongID());
			if (user == null) {
				synchronized (shards) {
					user = DiscordUtils.getUserFromJSON(shards.cursor().elem(), owner);
				}
			}

			return user;
		} catch (RateLimitException e) {
			Discord4J.LOGGER.error(LogMarkers.API, "Discord4J Internal Exception", e);
		}
		return null;
	}

	private String obtainGateway() {
		String gateway = null;
		try {
			GatewayResponse response = REQUESTS.GET.makeRequest(DiscordEndpoints.GATEWAY, GatewayResponse.class);
			gateway = response.url + "?encoding=etf&v=5";
		} catch (RateLimitException | DiscordException e) {
			Discord4J.LOGGER.error(LogMarkers.API, "Discord4J Internal Exception", e);
		}
		Discord4J.LOGGER.debug(LogMarkers.API, "Obtained gateway {}.", gateway);
		return gateway;
	}

	private void validateToken() throws DiscordException, RateLimitException {
		REQUESTS.GET.makeRequest(DiscordEndpoints.USERS + "@me");
	}

	// Sharding delegation

	@Override
	public void login() throws DiscordException, RateLimitException {
		synchronized (shards) {
			if (!shards.isEmpty()) {
				throw new DiscordException("Attempt to login client more than once.");
			}
		}

		validateToken();

		String gateway = obtainGateway();
		new RequestBuilder(this).setAsync(true).doAction(() -> {
			for (int i = 0; i < shardCount; i++) {
				synchronized (shards) {
					ShardImpl shard = new ShardImpl(this, gateway, i, shardCount);
					shards.add(shard);
					shard.login();
					
					getDispatcher().waitFor(ShardReadyEvent.class);
					
					if (i != shardCount-1) { // all but last
						Discord4J.LOGGER.trace(LogMarkers.API, "Sleeping for login ratelimit.");
						Thread.sleep(5000);
					}
				}
			}
			getDispatcher().dispatch(new ReadyEvent());
			return true;
		}).build();

		if (!isDaemon) {
			if (keepAlive == null) keepAlive = new Timer("DiscordClientImpl Keep Alive");
			keepAlive.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					Discord4J.LOGGER.trace(LogMarkers.API, "DiscordClientImpl Keep Alive");
				}
			}, 0, 10000);
		}
	}

	@Override
	public void logout() throws DiscordException {
		synchronized (shards) {
			for (IShard shard : shards) {
				shard.logout();
			}
			shards.clear();
		}
		if (keepAlive != null)
			keepAlive.cancel();
	}

	@Override
	public boolean isLoggedIn() {
		synchronized (shards) {
			return shards.size() == getShardCount() && shards.stream().map(IShard::isLoggedIn).allMatch(bool -> bool);
		}
	}

	@Override
	public boolean isReady() {
		synchronized (shards) {
			return shards.size() == getShardCount() && shards.stream().map(IShard::isReady).allMatch(bool -> bool);
		}
	}

	@Override
	@Deprecated
	public void changeStatus(Status status) {
		// old functionality just in case
		synchronized (shards) {
			shards.forEach(s -> s.changeStatus(status));
		}
	}

	@Override
	@Deprecated
	public void changePresence(boolean isIdle) {
		// old functionality just in case
		synchronized (shards) {
			shards.forEach(s -> s.changePresence(isIdle));
		}
	}

	@Override
	public void changePlayingText(String playingText) {
		synchronized (shards) {
			shards.forEach(s -> s.changePlayingText(playingText));
		}
	}

	@Override
	public void online(String playingText) {
		synchronized (shards) {
			shards.forEach(s -> s.online(playingText));
		}
	}

	@Override
	public void online() {
		synchronized (shards) {
			shards.forEach(IShard::online);
		}
	}

	@Override
	public void idle(String playingText) {
		synchronized (shards) {
			shards.forEach(s -> s.idle(playingText));
		}
	}

	@Override
	public void idle() {
		synchronized (shards) {
			shards.forEach(IShard::idle);
		}
	}

	@Override
	public void streaming(String playingText, String streamingUrl) {
		synchronized (shards) {
			shards.forEach(s -> s.streaming(playingText, streamingUrl));
		}
	}

	@Override
	public List<IGuild> getGuilds() {
		synchronized (shards) {
			return shards.stream()
					.map(IShard::getGuilds)
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}
	}

	@Override
	public IGuild getGuildByID(String guildID) {
		return getGuilds().stream()
				.filter(g -> g.getID().equals(guildID))
				.findFirst().orElse(null);
	}
	
	@Override
	public IGuild getGuildByID(long guildID) {
		return getGuilds().stream()
				.filter(g -> g.getLongID() == guildID)
				.findFirst().orElse(null);
	}

	@Override
	public List<IChannel> getChannels(boolean includePrivate) {
		synchronized (shards) {
			return shards.stream()
					.map(c -> c.getChannels(includePrivate))
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}
	}

	@Override
	public List<IChannel> getChannels() {
		synchronized (shards) {
			return shards.stream()
					.map(IShard::getChannels)
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}
	}

	@Override
	public IChannel getChannelByID(String channelID) {
		return getChannels(true).stream()
				.filter(c -> c.getID().equals(channelID))
				.findFirst().orElse(null);
	}
	
	@Override
	public IChannel getChannelByID(long channelID) {
		return getChannels(true).stream()
				.filter(c -> c.getLongID() == channelID)
				.findFirst().orElse(null);
	}

	@Override
	public List<IVoiceChannel> getVoiceChannels() {
		synchronized (shards) {
			return shards.stream()
					.map(IShard::getVoiceChannels)
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}
	}

	@Override
	public List<IVoiceChannel> getConnectedVoiceChannels() {
		return getOurUser().getConnectedVoiceChannels();
	}

	@Override
	public IVoiceChannel getVoiceChannelByID(String id) {
		return getVoiceChannels().stream()
				.filter(vc -> vc.getID().equals(id))
				.findFirst().orElse(null);
	}
	
	@Override
	public IVoiceChannel getVoiceChannelByID(long id) {
		return getVoiceChannels().stream()
				.filter(vc -> vc.getLongID() == id)
				.findFirst().orElse(null);
	}

	@Override
	public List<IUser> getUsers() {
		synchronized (shards) {
			return shards.stream()
					.map(IShard::getUsers)
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}
	}

	@Override
	public IUser getUserByID(String userID) {
		return getUsers().stream()
				.filter(u -> u.getID().equals(userID))
				.findFirst().orElse(null);
	}
	
	@Override
	public IUser getUserByID(long userID) {
		return getUsers().stream()
				.filter(u -> u.getLongID() == userID)
				.findFirst().orElse(null);
	}

	@Override
	public List<IRole> getRoles() {
		synchronized (shards) {
			return shards.stream()
					.map(IShard::getRoles)
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}
	}

	@Override
	public IRole getRoleByID(String roleID) {
		return getRoles().stream()
				.filter(r -> r.getID().equals(roleID))
				.findFirst().orElse(null);
	}
	
	@Override
	public IRole getRoleByID(long roleID) {
		return getRoles().stream()
				.filter(r -> r.getLongID() == roleID)
				.findFirst().orElse(null);
	}

	@Override
	public List<IMessage> getMessages(boolean includePrivate) {
		synchronized (shards) {
			return shards.stream()
					.map(c -> c.getMessages(includePrivate))
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}
	}

	@Override
	public List<IMessage> getMessages() {
		synchronized (shards) {
			return shards.stream()
					.map(IShard::getMessages)
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}
	}

	@Override
	public IMessage getMessageByID(String messageID) {
		return getMessages(true).stream()
				.filter(m -> m.getID().equals(messageID))
				.findFirst().orElse(null);
	}
	
	@Override
	public IMessage getMessageByID(long messageID) {
		return getMessages(true).stream()
				.filter(m -> m.getLongID() == messageID)
				.findFirst().orElse(null);
	}

	@Override
	public IPrivateChannel getOrCreatePMChannel(IUser user) throws DiscordException, RateLimitException {
		synchronized (shards) {
			IShard shard = shards.stream().filter(s -> s.getUserByID(user.getLongID()) != null).findFirst().get();
			return shard.getOrCreatePMChannel(user);
		}
	}

	@Override
	public IInvite getInviteForCode(String code) {
		if (!isReady()) {
			Discord4J.LOGGER.error(LogMarkers.API, "Attempt to get invite before bot is ready!");
			return null;
		}

		InviteObject invite;
		try {
			invite = DiscordUtils.MAPPER.readValue(REQUESTS.GET.makeRequest(DiscordEndpoints.INVITE + code), InviteObject.class);
		} catch (DiscordException | RateLimitException | IOException e) {
			Discord4J.LOGGER.error(LogMarkers.API, "Encountered error while retrieving invite: ", e);
			return null;
		}

		return invite == null ? null : DiscordUtils.getInviteFromJSON(this, invite);
	}

	public int getRetryCount() {
		return retryCount;
	}

	public int getMaxCacheCount() {
		return maxCacheCount;
	}
}
