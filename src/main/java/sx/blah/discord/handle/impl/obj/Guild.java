package sx.blah.discord.handle.impl.obj;

import com.koloboke.collect.map.hash.HashLongObjMap;
import com.koloboke.collect.map.hash.HashLongObjMaps;
import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.collect.set.hash.HashObjSets;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.IShard;
import sx.blah.discord.api.internal.DiscordClientImpl;
import sx.blah.discord.api.internal.DiscordEndpoints;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.api.internal.json.objects.*;
import sx.blah.discord.api.internal.json.requests.ChannelCreateRequest;
import sx.blah.discord.api.internal.json.requests.GuildEditRequest;
import sx.blah.discord.api.internal.json.requests.MemberEditRequest;
import sx.blah.discord.api.internal.json.requests.ReorderRolesRequest;
import sx.blah.discord.api.internal.json.responses.PruneResponse;
import sx.blah.discord.handle.audio.IAudioManager;
import sx.blah.discord.handle.audio.impl.AudioManager;
import sx.blah.discord.handle.impl.events.WebhookCreateEvent;
import sx.blah.discord.handle.impl.events.WebhookDeleteEvent;
import sx.blah.discord.handle.impl.events.WebhookUpdateEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class Guild implements IGuild {
	/**
	 * All text channels in the guild.
	 */
	protected final HashLongObjMap<IChannel> channels;

	/**
	 * All voice channels in the guild.
	 */
	protected final HashLongObjMap<IVoiceChannel> voiceChannels;

	/**
	 * All users connected to the guild.
	 */
	public final HashLongObjMap<IUser> users;

	/**
	 * The joined timetamps for users.
	 */
	public final HashLongObjMap<LocalDateTime> joinTimes;

	/**
	 * The name of the guild.
	 */
	protected volatile String name;

	/**
	 * The ID of this guild.
	 */
	protected final long id;

	/**
	 * The location of the guild icon
	 */
	protected volatile String icon;

	/**
	 * The url pointing to the guild icon
	 */
	protected volatile String iconURL;

	/**
	 * The user id for the owner of the guild
	 */
	protected volatile long ownerID;

	/**
	 * The roles the guild contains.
	 */
	protected final HashLongObjMap<IRole> roles;

	/**
	 * The channel where those who are afk are moved to.
	 */
	protected volatile long afkChannel;
	/**
	 * The time in seconds for a user to be idle to be determined as "afk".
	 */
	protected volatile int afkTimeout;

	/**
	 * The region this guild is located in.
	 */
	protected volatile String regionID;

	/**
	 * The verification level of this guild
	 */
	protected volatile VerificationLevel verification;

	/**
	 * This guild's audio manager.
	 */
	protected volatile AudioManager audioManager;

	/**
	 * The client that created this object.
	 */
	protected final IDiscordClient client;

	/**
	 * The shard this object belongs to.
	 */
	private final IShard shard;

	/**
	 * The list of emojis.
	 */
	protected final HashObjSet<IEmoji> emojis;

	/**
	 * The total number of members in this guild
	 */
	private int totalMemberCount;

	public Guild(IShard shard, String name, long id, String icon, long ownerID, long afkChannel, int afkTimeout, String region, int verification) {
		this(shard, name, id, icon, ownerID, afkChannel, afkTimeout, region, verification, HashLongObjMaps.newMutableMap(), HashLongObjMaps.newMutableMap(), HashLongObjMaps.newMutableMap(), HashLongObjMaps.newMutableMap(), HashLongObjMaps.newMutableMap());
	}

	public Guild(IShard shard, String name, long id, String icon, long ownerID, long afkChannel, int afkTimeout, String region, int verification, HashLongObjMap<IRole> roles, HashLongObjMap<IChannel> channels, HashLongObjMap<IVoiceChannel> voiceChannels, HashLongObjMap<IUser> users, HashLongObjMap<LocalDateTime> joinTimes) {
		this.shard = shard;
		this.client = shard.getClient();
		this.name = name;
		this.voiceChannels = voiceChannels;
		this.channels = channels;
		this.users = users;
		this.id = id;
		this.icon = icon;
		this.joinTimes = joinTimes;
		this.iconURL = String.format(DiscordEndpoints.ICONS, Long.toUnsignedString(id), this.icon);
		this.ownerID = ownerID;
		this.roles = roles;
		this.afkChannel = afkChannel;
		this.afkTimeout = afkTimeout;
		this.regionID = region;
		this.verification = VerificationLevel.get(verification);
		this.audioManager = new AudioManager(this);
		this.emojis = HashObjSets.newMutableSet();
	}

	@Override
	public String getOwnerID() {
		return Long.toUnsignedString(ownerID);
	}
	
	@Override
	public long getOwnerLongID() {
		return ownerID;
	}

	@Override
	public IUser getOwner() {
		return client.getUserByID(ownerID);
	}

	/**
	 * Sets the CACHED owner id.
	 *
	 * @param id The user if of the new owner.
	 */
	public void setOwnerID(long id) {
		ownerID = id;
	}

	@Override
	public String getIcon() {
		return icon;
	}

	@Override
	public String getIconURL() {
		return iconURL;
	}

	/**
	 * Sets the CACHED icon id for the guild.
	 *
	 * @param icon The icon id.
	 */
	public void setIcon(String icon) {
		this.icon = icon;
		this.iconURL = String.format(DiscordEndpoints.ICONS, this.getStringID(), this.icon);
	}

	@Override
	public List<IChannel> getChannels() {
		synchronized (channels) {
			return new ArrayList<>(channels.values());
		}
	}

	@Override
	public IChannel getChannelByID(String id) {
		synchronized (channels) {
			return channels.get(Long.parseUnsignedLong(id));
		}
	}
	
	@Override
	public IChannel getChannelByID(long id) {
		synchronized (channels) {
			return channels.get(id);
		}
	}

	@Override
	public List<IUser> getUsers() {
		synchronized (users) {
			return new ArrayList<>(users.values());
		}
	}

	@Override
	public IUser getUserByID(String id) { //TODO add backup rest request to retrieve the user
		synchronized (users) {
			return users.get(Long.parseUnsignedLong(id));
		}
	}
	
	@Override
	public IUser getUserByID(long id) { //TODO add backup rest request to retrieve the user
		synchronized (users) {
			return users.get(id);
		}
	}

	@Override
	public List<IChannel> getChannelsByName(String name) {
		synchronized (channels) {
			return channels.values().stream().filter((channel) -> channel.getName().equals(name)).collect(Collectors.toList());
		}
	}

	@Override
	public List<IVoiceChannel> getVoiceChannelsByName(String name) {
		synchronized (voiceChannels) {
			return voiceChannels.values().stream().filter((channel) -> channel.getName().equals(name)).collect(Collectors.toList());
		}
	}

	@Override
	public List<IUser> getUsersByName(String name) {
		return getUsersByName(name, true);
	}

	@Override
	public List<IUser> getUsersByName(String name, boolean includeNicknames) {
		synchronized (users) {
			return users.values().stream().filter((user) -> user.getName().equals(name)
					|| (includeNicknames && user.getNicknameForGuild(this).orElse("").equals(name)))
					.collect(Collectors.toList());
		}
	}

	@Override
	public List<IUser> getUsersByRole(IRole role) {
		synchronized (users) {
			synchronized (roles) {
				return users.values().stream()
						.filter(user -> user.getRolesForGuild(this).contains(role))
						.collect(Collectors.toList());
			}
		}
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the CACHED name of the guild.
	 *
	 * @param name The name.
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public long getLongID() {
		return id;
	}
	
	/**
	 * CACHES a user to the guild.
	 *
	 * @param user The user.
	 */
	public void addUser(IUser user) {
		synchronized (users) {
			this.users.put(user.getLongID(), user);
		}
	}

	/**
	 * CACHES a channel to the guild.
	 *
	 * @param channel The channel.
	 */
	public void addChannel(IChannel channel) {
		synchronized (channels) {
			this.channels.put(channel.getLongID(), channel);
		}
	}

	@Override
	public List<IRole> getRoles() {
		synchronized (roles) {
			return new ArrayList<>(roles.values());
		}
	}

	@Override
	public List<IRole> getRolesForUser(IUser user) {
		synchronized (roles) {
			return user.getRolesForGuild(this);
		}
	}

	/**
	 * CACHES a role to the guild.
	 *
	 * @param role The role.
	 */
	public void addRole(IRole role) {
		synchronized (roles) {
			this.roles.put(role.getLongID(), role);
		}
	}

	@Override
	public IRole getRoleByID(String id) {
		synchronized (roles) {
			return roles.get(Long.parseUnsignedLong(id));
		}
	}
	
	@Override
	public IRole getRoleByID(long id) {
		synchronized (roles) {
			return roles.get(id);
		}
	}

	@Override
	public List<IRole> getRolesByName(String name) {
		synchronized (roles) {
			return roles.values().stream().filter((role) -> role.getName().equals(name)).collect(Collectors.toList());
		}
	}

	@Override
	public List<IVoiceChannel> getVoiceChannels() {
		synchronized (voiceChannels) {
			return new ArrayList<>(voiceChannels.values());
		}
	}

	@Override
	public IVoiceChannel getVoiceChannelByID(String id) {
		synchronized (voiceChannels) {
			return voiceChannels.get(Long.parseUnsignedLong(id));
		}
	}
	
	@Override
	public IVoiceChannel getVoiceChannelByID(long id) {
		synchronized (voiceChannels) {
			return voiceChannels.get(id);
		}
	}

	@Override
	public IVoiceChannel getAFKChannel() {
		return getVoiceChannelByID(afkChannel);
	}

	@Override
	public IVoiceChannel getConnectedVoiceChannel() {
		synchronized (voiceChannels) {
			return client.getConnectedVoiceChannels().stream()
					.filter((voiceChannels::containsValue))
					.findFirst().orElse(null);
		}
	}

	@Override
	public int getAFKTimeout() {
		return afkTimeout;
	}

	public void setAFKChannel(long id) {
		this.afkChannel = id;
	}

	public void setAfkTimeout(int timeout) {
		this.afkTimeout = timeout;
	}

	public void addVoiceChannel(IVoiceChannel channel) {
		synchronized (voiceChannels) {
			voiceChannels.put(channel.getLongID(), channel);
		}
	}

	@Override
	public IRole createRole() throws DiscordException, RateLimitException, MissingPermissionsException {
		DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_ROLES));

		RoleObject response = ((DiscordClientImpl) client).REQUESTS.POST.makeRequest(
				DiscordEndpoints.GUILDS+getStringID()+"/roles",
				RoleObject.class);
		return DiscordUtils.getRoleFromJSON(this, response);
	}

	@Override
	public List<IUser> getBannedUsers() throws DiscordException, RateLimitException {
		BanObject[] bans = ((DiscordClientImpl) client).REQUESTS.GET.makeRequest(
				DiscordEndpoints.GUILDS+getStringID()+"/bans",
				BanObject[].class);
		return Arrays.stream(bans).map(b -> DiscordUtils.getUserFromJSON(getShard(), b.user)).collect(Collectors.toList());
	}

	@Override
	public void banUser(IUser user) throws DiscordException, RateLimitException, MissingPermissionsException {
		banUser(user, 0);
	}

	@Override
	public void banUser(IUser user, int deleteMessagesForDays) throws DiscordException, RateLimitException, MissingPermissionsException {
		DiscordUtils.checkPermissions(client, this, getRolesForUser(user), EnumSet.of(Permissions.BAN));
		banUser(user.getStringID(), deleteMessagesForDays);
	}

	@Override
	public void banUser(String userID) throws DiscordException, RateLimitException, MissingPermissionsException {
		IUser user = getUserByID(userID);
		if (getUserByID(userID) == null) {
			DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.BAN));
		} else {
			DiscordUtils.checkPermissions(client, this, getRolesForUser(user), EnumSet.of(Permissions.BAN));
		}

		banUser(userID, 0);
	}

	@Override
	public void banUser(String userID, int deleteMessagesForDays) throws DiscordException, RateLimitException, MissingPermissionsException {
		((DiscordClientImpl) client).REQUESTS.PUT.makeRequest(DiscordEndpoints.GUILDS + getStringID() + "/bans/" + userID + "?delete-message-days=" + deleteMessagesForDays);
	}

	@Override
	public void pardonUser(String userID) throws DiscordException, RateLimitException, MissingPermissionsException {
		DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.BAN));
		((DiscordClientImpl) client).REQUESTS.DELETE.makeRequest(DiscordEndpoints.GUILDS+getStringID()+"/bans/"+userID);
	}

	@Override
	public void kickUser(IUser user) throws DiscordException, RateLimitException, MissingPermissionsException {
		DiscordUtils.checkPermissions(client, this, user.getRolesForGuild(this), EnumSet.of(Permissions.KICK));
		((DiscordClientImpl) client).REQUESTS.DELETE.makeRequest(DiscordEndpoints.GUILDS+getStringID()+"/members/"+user.getStringID());
	}

	@Override
	public void editUserRoles(IUser user, IRole[] roles) throws DiscordException, RateLimitException, MissingPermissionsException {
		DiscordUtils.checkPermissions(client, this, Arrays.asList(roles), EnumSet.of(Permissions.MANAGE_ROLES));

		((DiscordClientImpl) client).REQUESTS.PATCH.makeRequest(
				DiscordEndpoints.GUILDS+id+"/members/"+user.getStringID(), new MemberEditRequest(roles));

	}

	@Override
	public void setDeafenUser(IUser user, boolean deafen) throws DiscordException, RateLimitException, MissingPermissionsException {
		DiscordUtils.checkPermissions(client, this, user.getRolesForGuild(this), EnumSet.of(Permissions.VOICE_DEAFEN_MEMBERS));

		((DiscordClientImpl) client).REQUESTS.PATCH.makeRequest(
				DiscordEndpoints.GUILDS+getStringID()+"/members/"+user.getStringID(), new MemberEditRequest(deafen));
	}

	@Override
	public void setMuteUser(IUser user, boolean mute) throws DiscordException, RateLimitException, MissingPermissionsException {
		DiscordUtils.checkPermissions(client, this, user.getRolesForGuild(this), EnumSet.of(Permissions.VOICE_MUTE_MEMBERS));

		((DiscordClientImpl) client).REQUESTS.PATCH.makeRequest(
				DiscordEndpoints.GUILDS+getStringID()+"/members/"+user.getStringID(), new MemberEditRequest(mute, true));
	}

	@Override
	public void setUserNickname(IUser user, String nick) throws DiscordException, RateLimitException, MissingPermissionsException {
		boolean isSelf = user.equals(client.getOurUser());
		if (isSelf) {
			DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.CHANGE_NICKNAME));
		} else {
			DiscordUtils.checkPermissions(client, this, user.getRolesForGuild(this), EnumSet.of(Permissions.MANAGE_NICKNAMES));
		}

		((DiscordClientImpl) client).REQUESTS.PATCH.makeRequest(
				DiscordEndpoints.GUILDS+getStringID()+"/members/"+(isSelf ? "@me/nick" : user.getStringID()),
				new MemberEditRequest(nick == null ? "" : nick, true));
	}

	@Override
	public void edit(String name, IRegion region, VerificationLevel level, Image icon, IVoiceChannel afkChannel, int afkTimeout) throws DiscordException, RateLimitException, MissingPermissionsException {
		DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_SERVER));

		if (name == null || name.length() < 2 || name.length() > 100)
			throw new IllegalArgumentException("Guild name must be between 2 and 100 characters!");
		if (region == null)
			throw new IllegalArgumentException("Region must not be null.");
		if (level == null)
			throw new IllegalArgumentException("Verification level must not be null.");
		if (icon == null)
			throw new IllegalArgumentException("Icon must not be null.");
		if (afkChannel != null && !getVoiceChannels().contains(afkChannel))
			throw new IllegalArgumentException("Invalid AFK voice channel.");
		if (afkTimeout != 60 && afkTimeout != 300 && afkTimeout != 900 && afkTimeout != 1800 && afkTimeout != 3600)
			throw new IllegalArgumentException("AFK timeout must be one of (60, 300, 900, 1800, 3600).");

		((DiscordClientImpl) client).REQUESTS.PATCH.makeRequest(
				DiscordEndpoints.GUILDS + id, new GuildEditRequest(name, region.getID(), level.ordinal(), icon.getData(),
						afkChannel == null ? null : afkChannel.getStringID(), afkTimeout));
	}

	@Override
	public void changeName(String name) throws DiscordException, RateLimitException, MissingPermissionsException {
		edit(name, getRegion(), getVerificationLevel(), this::getIcon, getAFKChannel(), getAFKTimeout());
	}

	@Override
	public void changeRegion(IRegion region) throws DiscordException, RateLimitException, MissingPermissionsException {
		edit(getName(), region, getVerificationLevel(), this::getIcon, getAFKChannel(), getAFKTimeout());
	}

	@Override
	public void changeVerificationLevel(VerificationLevel verificationLevel) throws DiscordException, RateLimitException, MissingPermissionsException {
		edit(getName(), getRegion(), verificationLevel, this::getIcon, getAFKChannel(), getAFKTimeout());
	}

	@Override
	public void changeIcon(Image icon) throws DiscordException, RateLimitException, MissingPermissionsException {
		edit(getName(), getRegion(), getVerificationLevel(), icon, getAFKChannel(), getAFKTimeout());
	}

	@Override
	public void changeAFKChannel(IVoiceChannel afkChannel) throws DiscordException, RateLimitException, MissingPermissionsException {
		edit(getName(), getRegion(), getVerificationLevel(), this::getIcon, afkChannel, getAFKTimeout());
	}

	@Override
	public void changeAFKTimeout(int timeout) throws DiscordException, RateLimitException, MissingPermissionsException {
		edit(getName(), getRegion(), getVerificationLevel(), this::getIcon, getAFKChannel(), timeout);
	}

	@Override
	@Deprecated
	public void deleteGuild() throws DiscordException, RateLimitException, MissingPermissionsException {
		if (ownerID != client.getOurUser().getLongID())
			throw new MissingPermissionsException("You must be the guild owner to delete guilds!", EnumSet.noneOf(Permissions.class));

		((DiscordClientImpl) client).REQUESTS.DELETE.makeRequest(DiscordEndpoints.GUILDS+getStringID());
	}

	@Override
	@Deprecated
	public void leaveGuild() throws DiscordException, RateLimitException {
		if (ownerID == client.getOurUser().getLongID())
			throw new DiscordException("Guild owners cannot leave their own guilds! Use deleteGuild() instead.");

		((DiscordClientImpl) client).REQUESTS.DELETE.makeRequest(DiscordEndpoints.USERS+"@me/guilds/"+getStringID());
	}

	@Override
	public void leave() throws DiscordException, RateLimitException {
		((DiscordClientImpl) client).REQUESTS.DELETE.makeRequest(DiscordEndpoints.USERS+"@me/guilds/"+getStringID());
	}

	@Override
	public IChannel createChannel(String name) throws DiscordException, RateLimitException, MissingPermissionsException {
		shard.checkReady("create channel");
		DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_CHANNELS));

		if (name == null || name.length() < 2 || name.length() > 100)
			throw new DiscordException("Channel name can only be between 2 and 100 characters!");

		ChannelObject response = ((DiscordClientImpl) client).REQUESTS.POST.makeRequest(
				DiscordEndpoints.GUILDS+getStringID()+"/channels",
				new ChannelCreateRequest(name, "text"),
				ChannelObject.class);

		IChannel channel = DiscordUtils.getChannelFromJSON(this, response);
		addChannel(channel);

		return channel;
	}

	@Override
	public IVoiceChannel createVoiceChannel(String name) throws DiscordException, RateLimitException, MissingPermissionsException {
		getShard().checkReady("create voice channel");
		DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_CHANNELS));

		if (name == null || name.length() < 2 || name.length() > 100)
			throw new DiscordException("Channel name can only be between 2 and 100 characters!");

		ChannelObject response = ((DiscordClientImpl) client).REQUESTS.POST.makeRequest(
				DiscordEndpoints.GUILDS+getStringID()+"/channels",
				new ChannelCreateRequest(name, "voice"),
				ChannelObject.class);

		IVoiceChannel channel = DiscordUtils.getVoiceChannelFromJSON(this, response);
		addVoiceChannel(channel);

		return channel;
	}

	@Override
	public IRegion getRegion() {
		return client.getRegionByID(regionID);
	}

	/**
	 * CACHES the region for this guild.
	 *
	 * @param regionID The region.
	 */
	public void setRegion(String regionID) {
		this.regionID = regionID;
	}

	@Override
	public VerificationLevel getVerificationLevel() {
		return verification;
	}

	/**
	 * CACHES the verification for this guild.
	 *
	 * @param verification The verification level.
	 */
	public void setVerificationLevel(int verification) {
		this.verification = VerificationLevel.get(verification);
	}

	@Override
	public IRole getEveryoneRole() {
		return getRoleByID(this.id);
	}

	@Override
	public IChannel getGeneralChannel() {
		return getChannelByID(this.id);
	}

	@Override
	public List<IInvite> getInvites() throws DiscordException, RateLimitException, MissingPermissionsException {
		DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_SERVER));
		ExtendedInviteObject[] response = ((DiscordClientImpl) client).REQUESTS.GET.makeRequest(
				DiscordEndpoints.GUILDS+ getStringID() + "/invites",
				ExtendedInviteObject[].class);

		List<IInvite> invites = new ArrayList<>();
		for (ExtendedInviteObject inviteResponse : response)
			invites.add(DiscordUtils.getInviteFromJSON(client, inviteResponse));

		return invites;
	}

	@Override
	public void reorderRoles(IRole... rolesInOrder) throws DiscordException, RateLimitException, MissingPermissionsException {
		if (rolesInOrder.length != getRoles().size())
			throw new DiscordException("The number of roles to reorder does not equal the number of available roles!");

		DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_ROLES));

		ReorderRolesRequest[] request = new ReorderRolesRequest[rolesInOrder.length];

		for (int i = 0; i < rolesInOrder.length; i++) {
			int position = rolesInOrder[i].getName().equals("@everyone") ? -1 : i+1;
			if (position != rolesInOrder[i].getPosition()) {
				IRole highest = getRolesForUser(client.getOurUser()).stream().sorted((o1, o2) -> {
					if (o1.getPosition() < o2.getPosition()) {
						return -1;
					} else if (o2.getPosition() < o1.getPosition()) {
						return 1;
					} else {
						return 0;
					}
				}).findFirst().orElse(null);
				if (highest != null && highest.getPosition() <= position)
					throw new MissingPermissionsException("Cannot edit the position of a role with a higher/equal position as your user's highest role.", EnumSet.noneOf(Permissions.class));
			}
			request[i] = new ReorderRolesRequest(rolesInOrder[i].getStringID(), position);
		}

		((DiscordClientImpl) client).REQUESTS.PATCH.makeRequest(DiscordEndpoints.GUILDS + getStringID() + "/roles", request);
	}

	@Override
	public int getUsersToBePruned(int days) throws DiscordException, RateLimitException {
		PruneResponse response = ((DiscordClientImpl) client).REQUESTS.GET.makeRequest(
				DiscordEndpoints.GUILDS + getStringID() + "/prune?days=" + days,
				PruneResponse.class);
		return response.pruned;
	}

	@Override
	public int pruneUsers(int days) throws DiscordException, RateLimitException {
		PruneResponse response = ((DiscordClientImpl) client).REQUESTS.POST.makeRequest(
				DiscordEndpoints.GUILDS + getStringID() + "/prune?days=" + days,
				PruneResponse.class);
		return response.pruned;
	}

	@Override
	public boolean isDeleted() {
		return getClient().getGuildByID(id) != this;
	}

	@Override
	public IAudioManager getAudioManager() {
		return audioManager;
	}

	@Override
	public LocalDateTime getJoinTimeForUser(IUser user) throws DiscordException {
		synchronized (joinTimes) {
			if (!joinTimes.containsKey(user.getLongID()))
				throw new DiscordException("Cannot find user "+user.getDisplayName(this)+" in this guild!");
			
			return joinTimes.get(user.getLongID());
		}
	}

	@Override
	public IMessage getMessageByID(String id) {
		synchronized (channels) {
			IMessage message = channels.values().stream()
					.map(IChannel::getMessageHistory)
					.flatMap(List::stream)
					.filter(msg -> msg.getLongID() == Long.parseUnsignedLong(id))
					.findFirst().orElse(null);
			
			if (message == null) {
				for (IChannel channel : channels.values()) {
					message = channel.getMessageByID(id);
					if (message != null)
						return message;
				}
			}
			
			return message;
		}
	}
	
	@Override
	public IMessage getMessageByID(long id) {
		synchronized (channels) {
			IMessage message = channels.values().stream()
					.map(IChannel::getMessageHistory)
					.flatMap(List::stream)
					.filter(msg -> msg.getLongID() == id)
					.findAny().orElse(null);
			
			if (message == null) {
				for (IChannel channel : channels.values()) {
					message = channel.getMessageByID(id);
					if (message != null)
						return message;
				}
			}
			
			return message;
		}
	}

	@Override
	public IDiscordClient getClient() {
		return client;
	}

	@Override
	public IShard getShard() {
		return shard;
	}

	@Override
	public synchronized IGuild copy() {
		return new Guild(shard, name, id, icon, ownerID, afkChannel, afkTimeout, regionID, verification.ordinal(),
				roles, channels, voiceChannels, users, joinTimes);
	}

	@Override
	public List<IEmoji> getEmojis() {
		synchronized (emojis) {
			return new ArrayList<>(emojis);
		}
	}

	@Override
	public IEmoji getEmojiByID(String idToUse) {
		synchronized (emojis) {
			return emojis.stream().filter(iemoji -> iemoji.getID().equals(idToUse)).findFirst().orElse(null);
		}
	}
	
	@Override
	public IEmoji getEmojiByID(long idToUse) {
		synchronized (emojis) {
			return emojis.stream().filter(iemoji -> iemoji.getLongID() == idToUse).findFirst().orElse(null);
		}
	}

	@Override
	public IEmoji getEmojiByName(String name) {
		synchronized (emojis) {
			return emojis.stream().filter(iemoji -> iemoji.getName().equals(name)).findFirst().orElse(null);
		}
	}

	@Override
	public IWebhook getWebhookByID(String id) {
		synchronized (channels) {
			return channels.values().stream()
					.map(IChannel::getWebhooks)
					.flatMap(List::stream)
					.filter(hook -> hook.getID().equals(id))
					.findAny().orElse(null);
		}
	}
	
	@Override
	public IWebhook getWebhookByID(long id) {
		synchronized (channels) {
			return channels.values().stream()
					.map(IChannel::getWebhooks)
					.flatMap(List::stream)
					.filter(hook -> hook.getLongID() == id)
					.findAny().orElse(null);
		}
	}

	@Override
	public List<IWebhook> getWebhooksByName(String name) {
		synchronized (channels) {
			return channels.values().stream()
					.map(IChannel::getWebhooks)
					.flatMap(List::stream)
					.filter(hook -> hook.getDefaultName().equals(name))
					.collect(Collectors.toList());
		}
	}

	@Override
	public List<IWebhook> getWebhooks() {
		synchronized (channels) {
			return channels.values().stream()
					.map(IChannel::getWebhooks)
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}
	}

	public void loadWebhooks() {
		try {
			DiscordUtils.checkPermissions(getClient(), this, EnumSet.of(Permissions.MANAGE_WEBHOOKS));
		} catch (MissingPermissionsException ignored) {
			return;
		}

		RequestBuffer.request(() -> {
			try {
				List<IWebhook> oldList = getWebhooks()
						.stream()
						.map(IWebhook::copy)
						.collect(Collectors.toCollection(CopyOnWriteArrayList::new));

				WebhookObject[] response = ((DiscordClientImpl) client).REQUESTS.GET.makeRequest(
						DiscordEndpoints.GUILDS + getStringID() + "/webhooks",
						WebhookObject[].class);

				if (response != null) {
					for (WebhookObject webhookObject : response) {
						Channel channel = (Channel) getChannelByID(webhookObject.getLongChannelID());
						if (getWebhookByID(webhookObject.getLongID()) == null) {
							IWebhook newWebhook = DiscordUtils.getWebhookFromJSON(channel, webhookObject);
							client.getDispatcher().dispatch(new WebhookCreateEvent(newWebhook));
							channel.addWebhook(newWebhook);
						} else {
							IWebhook toUpdate = channel.getWebhookByID(webhookObject.getLongID());
							IWebhook oldWebhook = toUpdate.copy();
							toUpdate = DiscordUtils.getWebhookFromJSON(channel, webhookObject);
							if (!oldWebhook.getDefaultName().equals(toUpdate.getDefaultName()) || !String.valueOf(oldWebhook.getDefaultAvatar()).equals(String.valueOf(toUpdate.getDefaultAvatar())))
								client.getDispatcher().dispatch(new WebhookUpdateEvent(oldWebhook, toUpdate));

							oldList.remove(oldWebhook);
						}
					}
				}

				oldList.forEach(webhook -> {
					((Channel) webhook.getChannel()).removeWebhook(webhook);
					client.getDispatcher().dispatch(new WebhookDeleteEvent(webhook));
				});
			} catch (Exception e) {
				Discord4J.LOGGER.warn(LogMarkers.HANDLE, "Discord4J Internal Exception", e);
			}
		});
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;

		return this.getClass().isAssignableFrom(other.getClass()) && ((IGuild) other).getID().equals(getID());
	}

	@Override
	public int getTotalMemberCount() {
		return totalMemberCount;
	}

	public void setTotalMemberCount(int totalMemberCount){
		this.totalMemberCount = totalMemberCount;
	}
}
