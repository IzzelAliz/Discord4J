package sx.blah.discord.api.internal;

import com.austinv11.etf.erlang.ErlangMap;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.internal.etf.event.*;
import sx.blah.discord.api.internal.json.objects.*;
import sx.blah.discord.api.internal.etf.event.ReadyResponse;
import sx.blah.discord.api.internal.etf.voice.VoiceUpdateResponse;
import sx.blah.discord.handle.impl.events.*;
import sx.blah.discord.handle.impl.obj.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.LogMarkers;
import sx.blah.discord.util.MessageList;
import sx.blah.discord.util.RequestBuilder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static sx.blah.discord.api.internal.DiscordUtils.ETF_MAPPER;

class DispatchHandler {
	private final DiscordWS ws;
	private final ShardImpl shard;
	private final DiscordClientImpl client;

	DispatchHandler(DiscordWS ws, ShardImpl shard) {
		this.ws = ws;
		this.shard = shard;
		this.client = (DiscordClientImpl) shard.getClient();
	}

	public void handle(byte[] payload) {
		ErlangMap map = DiscordUtils.PARTIAL_ETF_CONFIG.createParser(payload).nextMap().getErlangMap("d");
		String type = DiscordUtils.PARTIAL_ETF_CONFIG.createParser(payload).nextMap().getString("t");
		switch (type) {
			case "RESUMED": resumed(); break;
			case "READY": ready(ETF_MAPPER.read(map, ReadyResponse.class)); break;
			case "MESSAGE_CREATE": messageCreate(ETF_MAPPER.read(map, MessageObject.class)); break;
			case "TYPING_START": typingStart(ETF_MAPPER.read(map, TypingEventResponse.class)); break;
			case "GUILD_CREATE": guildCreate(ETF_MAPPER.read(map, GuildObject.class)); break;
			case "GUILD_MEMBER_ADD": guildMemberAdd(ETF_MAPPER.read(map, GuildMemberAddEventResponse.class)); break;
			case "GUILD_MEMBER_REMOVE": guildMemberRemove(ETF_MAPPER.read(map, GuildMemberRemoveEventResponse.class)); break;
			case "GUILD_MEMBER_UPDATE": guildMemberUpdate(ETF_MAPPER.read(map, GuildMemberUpdateEventResponse.class)); break;
			case "MESSAGE_UPDATE": messageUpdate(ETF_MAPPER.read(map, MessageObject.class)); break;
			case "MESSAGE_DELETE": messageDelete(ETF_MAPPER.read(map, MessageDeleteEventResponse.class)); break;
			case "MESSAGE_DELETE_BULK": messageDeleteBulk(ETF_MAPPER.read(map, MessageDeleteBulkEventResponse.class)); break;
			case "PRESENCE_UPDATE": presenceUpdate(ETF_MAPPER.read(map, PresenceUpdateEventResponse.class)); break;
			case "GUILD_DELETE": guildDelete(ETF_MAPPER.read(map, GuildObject.class)); break;
			case "CHANNEL_CREATE": channelCreate(map); break;
			case "CHANNEL_DELETE": channelDelete(ETF_MAPPER.read(map, ChannelObject.class)); break;
			case "CHANNEL_PINS_UPDATE": /* Implemented in MESSAGE_UPDATE. Ignored */ break;
			case "CHANNEL_PINS_ACK": /* Ignored */ break;
			case "USER_UPDATE": userUpdate(ETF_MAPPER.read(map, UserUpdateEventResponse.class)); break;
			case "CHANNEL_UPDATE": channelUpdate(ETF_MAPPER.read(map, ChannelObject.class)); break;
			case "GUILD_MEMBERS_CHUNK": guildMembersChunk(ETF_MAPPER.read(map, GuildMemberChunkEventResponse.class)); break;
			case "GUILD_UPDATE": guildUpdate(ETF_MAPPER.read(map, GuildObject.class)); break;
			case "GUILD_ROLE_CREATE": guildRoleCreate(ETF_MAPPER.read(map, GuildRoleEventResponse.class)); break;
			case "GUILD_ROLE_UPDATE": guildRoleUpdate(ETF_MAPPER.read(map, GuildRoleEventResponse.class)); break;
			case "GUILD_ROLE_DELETE": guildRoleDelete(ETF_MAPPER.read(map, GuildRoleDeleteEventResponse.class)); break;
			case "GUILD_BAN_ADD": guildBanAdd(ETF_MAPPER.read(map, GuildBanEventResponse.class)); break;
			case "GUILD_BAN_REMOVE": guildBanRemove(ETF_MAPPER.read(map, GuildBanEventResponse.class)); break;
			case "GUILD_EMOJIS_UPDATE": guildEmojisUpdate(ETF_MAPPER.read(map, GuildEmojiUpdateResponse.class)); break;
			case "GUILD_INTEGRATIONS_UPDATE": /* TODO: Impl Guild integrations */ break;
			case "VOICE_STATE_UPDATE": voiceStateUpdate(ETF_MAPPER.read(map, VoiceStateObject.class)); break;
			case "VOICE_SERVER_UPDATE": voiceServerUpdate(ETF_MAPPER.read(map, VoiceUpdateResponse.class)); break;
			case "MESSAGE_REACTION_ADD": reactionAdd(ETF_MAPPER.read(map, ReactionEventResponse.class)); break;
			case "MESSAGE_REACTION_REMOVE": reactionRemove(ETF_MAPPER.read(map, ReactionEventResponse.class)); break;
			case "MESSAGE_REACTION_REMOVE_ALL": /* REMOVE_ALL is 204 empty but REACTION_REMOVE is sent anyway */ break;
			case "WEBHOOKS_UPDATE": webhookUpdate(ETF_MAPPER.read(map, WebhookObject.class)); break;

			default:
				Discord4J.LOGGER.warn(LogMarkers.WEBSOCKET, "Unknown message received: {}, REPORT THIS TO THE DISCORD4J DEV!", type);
		}
	}

	private void ready(ReadyResponse ready) {
		Discord4J.LOGGER.info(LogMarkers.WEBSOCKET, "Connected to Discord Gateway v{}. Receiving {} guilds.", ready.v, ready.guilds.length);

		ws.state = DiscordWS.State.READY;
		ws.hasReceivedReady = true; // Websocket received actual ready event
		if (client.ourUser == null) client.ourUser = DiscordUtils.getUserFromJSON(shard, ready.user);
		client.getDispatcher().dispatch(new LoginEvent(shard));

		new RequestBuilder(client).setAsync(true).doAction(() -> {
			ws.sessionId = ready.session_id;

			if (MessageList.getEfficiency(client) == null) //User did not manually set the efficiency
				MessageList.setEfficiency(client, MessageList.EfficiencyLevel.getEfficiencyForGuilds(ready.guilds.length));

			Set<UnavailableGuildObject> waitingGuilds = ConcurrentHashMap.newKeySet(ready.guilds.length);
			waitingGuilds.addAll(Arrays.asList(ready.guilds));

			final AtomicInteger loadedGuilds = new AtomicInteger(0);
			client.getDispatcher().waitFor((GuildCreateEvent e) -> {
				waitingGuilds.removeIf(g -> g.getLongID() == e.getGuild().getLongID());
				return loadedGuilds.incrementAndGet() >= ready.guilds.length;
			}, 10, TimeUnit.SECONDS);

			waitingGuilds.forEach(guild -> client.getDispatcher().dispatch(new GuildUnavailableEvent(guild.getLongID())));
			return true;
		}).andThen(() -> {
			if (this.shard.getInfo()[0] == 0) { // pms are only sent to shard one
				Arrays.stream(ready.private_channels)
						.map(pm -> DiscordUtils.getPrivateChannelFromJSON(shard, pm))
						.forEach(pm -> shard.privateChannels.put(pm.getLongID(), pm));
			}

			ws.isReady = true;
			client.getDispatcher().dispatch(new ShardReadyEvent(shard)); // All information for this shard has been received
			return true;
		}).execute();
	}

	private void resumed() {
		Discord4J.LOGGER.info(LogMarkers.WEBSOCKET, "Session resumed on shard " + shard.getInfo()[0]);
		ws.hasReceivedReady = true; // Technically a lie but irrelevant in the case of a resume.
		ws.isReady = true;          //
		client.getDispatcher().dispatch(new ResumedEvent(shard));
	}

	private void messageCreate(MessageObject json) {
		boolean mentioned = json.mention_everyone;

		Channel channel = (Channel) client.getChannelByID(json.getLongChannelID());

		if (null != channel) {
			if (!mentioned) { //Not worth checking if already mentioned
				for (UserObject user : json.mentions) { //Check mention array for a mention
					if (client.getOurUser().getLongID() == user.getLongID()) {
						mentioned = true;
						break;
					}
				}
			}

			if (!mentioned) { //Not worth checking if already mentioned
				for (long role : json.getLongRoles()) { //Check roles for a mention
					if (client.getOurUser().getRolesForGuild(channel.getGuild()).contains(channel.getGuild().getRoleByID(role))) {
						mentioned = true;
						break;
					}
				}
			}

			IMessage message = DiscordUtils.getMessageFromJSON(channel, json);

			if (!channel.getMessageHistory().contains(message)) {
				Discord4J.LOGGER.debug(LogMarkers.EVENTS, "Message from: {} ({}) in channel ID {}: {}", message.getAuthor().getName(),
						json.author.getStringID(), json.getStringChannelID(), json.content);

				List<String> inviteCodes = DiscordUtils.getInviteCodesFromMessage(json.content);
				if (!inviteCodes.isEmpty()) {
					List<IInvite> invites = inviteCodes.stream()
							.map(s -> client.getInviteForCode(s))
							.filter(Objects::nonNull)
							.collect(Collectors.toList());
					if (!invites.isEmpty()) client.getDispatcher().dispatch(new InviteReceivedEvent(invites.toArray(new IInvite[invites.size()]), message));
				}

				if (mentioned) {
					client.dispatcher.dispatch(new MentionEvent(message));
				}

				channel.addToCache(message);

				if (message.getAuthor().equals(client.getOurUser())) {
					client.dispatcher.dispatch(new MessageSendEvent(message));
					message.getChannel().setTypingStatus(false); //Messages being sent should stop the bot from typing
				} else {
					client.dispatcher.dispatch(new MessageReceivedEvent(message));
					if (!message.getEmbedded().isEmpty()) {
						client.dispatcher.dispatch(new MessageEmbedEvent(message, new ArrayList<>()));
					}
				}
			}
		}
	}

	private void typingStart(TypingEventResponse event) {
		User user;
		Channel channel = (Channel) client.getChannelByID(event.channel_id);
		if (channel != null) {
			if (channel.isPrivate()) {
				user = (User) ((IPrivateChannel) channel).getRecipient();
			} else {
				user = (User) channel.getGuild().getUserByID(event.user_id);
			}

			if (user != null) {
				client.dispatcher.dispatch(new TypingEvent(user, channel));
			}
		}
	}

	private void guildCreate(GuildObject json) {
		if (json.unavailable) { //Guild can't be reached, so we ignore it
			Discord4J.LOGGER.warn(LogMarkers.WEBSOCKET, "Guild with id {} is unavailable, ignoring it. Is there an outage?", json.getStringID());
			return;
		}
		
		Guild guild;
		synchronized (shard) {
			guild = (Guild) DiscordUtils.getGuildFromJSON(shard, json);
		}
		synchronized (shard.guildList) {
			shard.guildList.put(guild.getLongID(), guild);
		}

		new RequestBuilder(client).setAsync(true).doAction(() -> {
			try {
				guild.loadWebhooks();
				if (json.large) {
					client.getDispatcher().waitFor((AllUsersReceivedEvent e) ->
							e.getGuild().getID().equals(guild.getID())
					);
				}
				client.dispatcher.dispatch(new GuildCreateEvent(guild));
				Discord4J.LOGGER.debug(LogMarkers.EVENTS, "New guild has been created/joined! \"{}\" with ID {} on shard {}.", guild.getName(), guild.getID(), shard.getInfo()[0]);
			} catch (InterruptedException e) {
				Discord4J.LOGGER.error(LogMarkers.EVENTS, "Wait for AllUsersReceivedEvent on guild create was interrupted.", e);
			}
			return true;
		}).execute();
	}

	private void guildMemberAdd(GuildMemberAddEventResponse event) {
		long guildID = event.guild_id;
		Guild guild = (Guild) client.getGuildByID(guildID);
		if (guild != null) {
			User user = (User) DiscordUtils.getUserFromGuildMemberResponse(guild, new MemberObject(event.user, event.roles));
			guild.addUser(user);
			guild.setTotalMemberCount(guild.getTotalMemberCount() + 1);
			LocalDateTime timestamp = DiscordUtils.convertFromTimestamp(event.joined_at);
			Discord4J.LOGGER.debug(LogMarkers.EVENTS, "User \"{}\" joined guild \"{}\".", user.getName(), guild.getName());
			client.dispatcher.dispatch(new UserJoinEvent(guild, user, timestamp));
		}
	}

	private void guildMemberRemove(GuildMemberRemoveEventResponse event) {
		long guildID = event.guild_id;
		Guild guild = (Guild) client.getGuildByID(guildID);
		if (guild != null) {
			User user = (User) guild.getUserByID(event.user.getLongID());
			if (user != null) {
				synchronized (guild.users) {
					guild.users.remove(user.getLongID());
				}
				synchronized (guild.joinTimes) {
					guild.joinTimes.remove(user.getLongID());
				}
				guild.setTotalMemberCount(guild.getTotalMemberCount() - 1);
				Discord4J.LOGGER.debug(LogMarkers.EVENTS, "User \"{}\" has been removed from or left guild \"{}\".", user.getName(), guild.getName());
				client.dispatcher.dispatch(new UserLeaveEvent(guild, user));
			}
		}
	}

	private void guildMemberUpdate(GuildMemberUpdateEventResponse event) {
		Guild guild = (Guild) client.getGuildByID(event.guild_id);
		User user = (User) client.getUserByID(event.user.getLongID());

		if (guild != null && user != null) {
			List<IRole> oldRoles = new ArrayList<>(user.getRolesForGuild(guild));
			boolean rolesChanged = oldRoles.size() != event.roles.length + 1;//Add one for the @everyone role
			if (!rolesChanged) {
				rolesChanged = oldRoles.stream().filter(role -> {
					if (role.equals(guild.getEveryoneRole()))
						return false;

					for (long roleID : event.roles) {
						if (role.getLongID() == roleID) {
							return false;
						}
					}

					return true;
				}).collect(Collectors.toList()).size() > 0;
			}

			if (rolesChanged) {
				user.getRolesForGuild(guild).clear();
				for (long role : event.roles)
					user.addRole(guild.getLongID(), guild.getRoleByID(role));

				user.addRole(guild.getLongID(), guild.getEveryoneRole());

				client.dispatcher.dispatch(new UserRoleUpdateEvent(guild, user, oldRoles, user.getRolesForGuild(guild)));

				if (user.equals(client.getOurUser()))
					guild.loadWebhooks();
			}

			if (!user.getNicknameForGuild(guild).equals(Optional.ofNullable(event.nick))) {
				String oldNick = user.getNicknameForGuild(guild).orElse(null);
				user.addNick(guild.getLongID(), event.nick);

				client.dispatcher.dispatch(new NickNameChangeEvent(guild, user, oldNick, event.nick));
			}
		}
	}

	private void messageUpdate(MessageObject json) {
		long id = json.getLongID();
		long channelID = json.getLongChannelID();

		Channel channel = (Channel) client.getChannelByID(channelID);
		if (channel == null)
			return;

		Message toUpdate = (Message) channel.getMessageByID(id);
		IMessage oldMessage = toUpdate != null ? toUpdate.copy() : null;

		toUpdate = (Message) DiscordUtils.getUpdatedMessageFromJSON(toUpdate, json);

		if (oldMessage != null && json.pinned != null && oldMessage.isPinned() && !json.pinned) {
			client.dispatcher.dispatch(new MessageUnpinEvent(toUpdate));
		} else if (oldMessage != null && json.pinned != null && !oldMessage.isPinned() && json.pinned) {
			client.dispatcher.dispatch(new MessagePinEvent(toUpdate));
		} else if (oldMessage != null && oldMessage.getEmbedded().size() < toUpdate.getEmbedded().size()) {
			client.dispatcher.dispatch(new MessageEmbedEvent(toUpdate, oldMessage.getEmbedded()));
		} else {
			client.dispatcher.dispatch(new MessageUpdateEvent(oldMessage, toUpdate));
		}
	}

	private void messageDelete(MessageDeleteEventResponse event) {
		long id = event.id;
		long channelID = event.channel_id;
		Channel channel = (Channel) client.getChannelByID(channelID);

		if (channel != null) {
			Message message = (Message) channel.getMessageByID(id);
			if (message != null) {
				if (message.isPinned()) {
					message.setPinned(false); //For consistency with the event
					client.dispatcher.dispatch(new MessageUnpinEvent(message));
				}
				message.setDeleted(true);
				channel.messages.remove(message);
				client.dispatcher.dispatch(new MessageDeleteEvent(message));
			}
		}
	}

	private void messageDeleteBulk(MessageDeleteBulkEventResponse event) { //TODO: maybe add a separate event for this?
		for (long id : event.ids) {
			messageDelete(new MessageDeleteEventResponse(id, event.channel_id));
		}
	}

	private void presenceUpdate(PresenceUpdateEventResponse event) {
		IPresence presence = DiscordUtils.getPresenceFromJSON(event);
		Guild guild = (Guild) client.getGuildByID(event.guild_id);
		if (guild != null) {
			User user = (User) guild.getUserByID(event.user.getLongID());
			if (user != null) {
				if (event.user.username != null) { //Full object was sent so there is a user change, otherwise all user fields but id would be null
					IUser oldUser = user.copy();
					user = DiscordUtils.getUserFromJSON(shard, event.user);
					client.dispatcher.dispatch(new UserUpdateEvent(oldUser, user));
				}

				if (!user.getPresence().equals(presence)) {
					IPresence oldPresence = user.getPresence();
					user.setPresence(presence);
					client.dispatcher.dispatch(new PresenceUpdateEvent(user, oldPresence, presence));
					Discord4J.LOGGER.debug(LogMarkers.EVENTS, "User \"{}\" changed presence to {}", user.getName(), user.getPresence());
				}
			}
		}
	}

	private void guildDelete(GuildObject json) {
		Guild guild = (Guild) client.getGuildByID(json.getLongID());

		// Clean up cache
		guild.getShard().getGuilds().remove(guild);
		client.getOurUser().getConnectedVoiceChannels().removeAll(guild.getVoiceChannels());
		synchronized (client.voiceConnections) {
			DiscordVoiceWS vWS = client.voiceConnections.get(guild.getLongID());
			if (vWS != null) {
				vWS.disconnect(VoiceDisconnectedEvent.Reason.LEFT_CHANNEL);
				client.voiceConnections.remove(guild.getLongID());
			}
		}

		if (json.unavailable) { //Guild can't be reached
			Discord4J.LOGGER.warn(LogMarkers.WEBSOCKET, "Guild with id {} is unavailable, is there an outage?", json.getStringID());
			client.dispatcher.dispatch(new GuildUnavailableEvent(json.getLongID()));
		} else {
			Discord4J.LOGGER.debug(LogMarkers.EVENTS, "You have been kicked from or left \"{}\"! :O", guild.getName());
			client.dispatcher.dispatch(new GuildLeaveEvent(guild));
		}
	}

	private void channelCreate(ErlangMap map) {
		boolean isPrivate = map.getBoolean("is_private");

		if (isPrivate) { // PM channel.
			PrivateChannelObject event = ETF_MAPPER.read(map, PrivateChannelObject.class);
			long id = event.getLongID();
			boolean contained = false;
			synchronized (shard.privateChannels) {
				for (IPrivateChannel privateChannel : shard.privateChannels.values()) {
					if (privateChannel.getLongID() == id)
						contained = true;
				}
			}

			if (contained)
				return; // we already have this PM channel; no need to create another.

			PrivateChannel pm = DiscordUtils.getPrivateChannelFromJSON(shard, event);
			shard.privateChannels.put(pm.getLongID(), pm);

		} else { // Regular channel.
			ChannelObject event = ETF_MAPPER.read(map, ChannelObject.class);
			String type = event.type;
			Guild guild = (Guild) client.getGuildByID(event.getLongGuildID());
			if (guild != null) {
				if (type.equalsIgnoreCase("text")) { //Text channel
					Channel channel = (Channel) DiscordUtils.getChannelFromJSON(guild, event);
					guild.addChannel(channel);
					client.dispatcher.dispatch(new ChannelCreateEvent(channel));
				} else if (type.equalsIgnoreCase("voice")) {
					VoiceChannel channel = (VoiceChannel) DiscordUtils.getVoiceChannelFromJSON(guild, event);
					guild.addVoiceChannel(channel);
					client.dispatcher.dispatch(new VoiceChannelCreateEvent(channel));
				}
			}
		}
	}

	private void channelDelete(ChannelObject json) {
		if (json.type.equalsIgnoreCase("text")) {
			Channel channel = (Channel) client.getChannelByID(json.getLongID());
			if (channel != null) {
				if (!channel.isPrivate())
					channel.getGuild().getChannels().remove(channel);
				else
					shard.privateChannels.remove(channel);
				client.dispatcher.dispatch(new ChannelDeleteEvent(channel));
			}
		} else if (json.type.equalsIgnoreCase("voice")) {
			VoiceChannel channel = (VoiceChannel) client.getVoiceChannelByID(json.getLongID());
			if (channel != null) {
				channel.getGuild().getVoiceChannels().remove(channel);
				client.dispatcher.dispatch(new VoiceChannelDeleteEvent(channel));
			}
		}
	}

	private void userUpdate(UserUpdateEventResponse event) {
		User newUser = (User) client.getUserByID(event.getLongID());
		if (newUser != null) {
			IUser oldUser = newUser.copy();
			newUser = DiscordUtils.getUserFromJSON(shard, event);
			client.dispatcher.dispatch(new UserUpdateEvent(oldUser, newUser));
		}
	}

	private void channelUpdate(ChannelObject json) {
		if (!json.is_private) {
			if (json.type.equalsIgnoreCase("text")) {
				Channel toUpdate = (Channel) client.getChannelByID(json.getLongID());
				if (toUpdate != null) {
					IChannel oldChannel = toUpdate.copy();

					toUpdate = (Channel) DiscordUtils.getChannelFromJSON(toUpdate.getGuild(), json);

					toUpdate.loadWebhooks();

					client.getDispatcher().dispatch(new ChannelUpdateEvent(oldChannel, toUpdate));
				}
			} else if (json.type.equalsIgnoreCase("voice")) {
				VoiceChannel toUpdate = (VoiceChannel) client.getVoiceChannelByID(json.getLongID());
				if (toUpdate != null) {
					VoiceChannel oldChannel = (VoiceChannel) toUpdate.copy();

					toUpdate = (VoiceChannel) DiscordUtils.getVoiceChannelFromJSON(toUpdate.getGuild(), json);

					client.getDispatcher().dispatch(new VoiceChannelUpdateEvent(oldChannel, toUpdate));
				}
			}
		}
	}

	private void guildMembersChunk(GuildMemberChunkEventResponse event) {
		Guild guildToUpdate = (Guild) client.getGuildByID(event.guild_id);
		if (guildToUpdate == null) {
			Discord4J.LOGGER.warn(LogMarkers.WEBSOCKET, "Can't receive guild members chunk for guild id {}, the guild is null!", event.guild_id);
			return;
		}

		for (MemberObject member : event.members) {
			IUser user = DiscordUtils.getUserFromGuildMemberResponse(guildToUpdate, member);
			guildToUpdate.addUser(user);
		}
		if (guildToUpdate.getUsers().size() == guildToUpdate.getTotalMemberCount()) {
			client.getDispatcher().dispatch(new AllUsersReceivedEvent(guildToUpdate));
		}
	}

	private void guildUpdate(GuildObject json) {
		Guild toUpdate = (Guild) client.getGuildByID(json.getLongID());

		if (toUpdate != null) {
			IGuild oldGuild = toUpdate.copy();

			toUpdate = (Guild) DiscordUtils.getGuildFromJSON(shard, json);

			if (!toUpdate.getOwnerID().equals(oldGuild.getOwnerID())) {
				client.dispatcher.dispatch(new GuildTransferOwnershipEvent(oldGuild.getOwner(), toUpdate.getOwner(), toUpdate));
			} else {
				client.dispatcher.dispatch(new GuildUpdateEvent(oldGuild, toUpdate));
			}
		}
	}

	private void guildRoleCreate(GuildRoleEventResponse event) {
		IGuild guild = client.getGuildByID(event.guild_id);
		if (guild != null) {
			IRole role = DiscordUtils.getRoleFromJSON(guild, event.role);
			client.dispatcher.dispatch(new RoleCreateEvent(role));
		}
	}

	private void guildRoleUpdate(GuildRoleEventResponse event) {
		IGuild guild = client.getGuildByID(event.guild_id);
		if (guild != null) {
			IRole toUpdate = guild.getRoleByID(event.role.getLongID());
			if (toUpdate != null) {
				IRole oldRole = toUpdate.copy();
				toUpdate = DiscordUtils.getRoleFromJSON(guild, event.role);
				client.dispatcher.dispatch(new RoleUpdateEvent(oldRole, toUpdate));

				if (guild.getRolesForUser(client.getOurUser()).contains(toUpdate))
					((Guild) guild).loadWebhooks();
			}
		}
	}

	private void guildRoleDelete(GuildRoleDeleteEventResponse event) {
		IGuild guild = client.getGuildByID(event.guild_id);
		if (guild != null) {
			IRole role = guild.getRoleByID(event.role_id);
			if (role != null) {
				guild.getRoles().remove(role);
				client.dispatcher.dispatch(new RoleDeleteEvent(role));
			}
		}
	}

	private void guildBanAdd(GuildBanEventResponse event) {
		IGuild guild = client.getGuildByID(event.guild_id);
		if (guild != null) {
			IUser user = DiscordUtils.getUserFromJSON(shard, event.user);
			if (client.getUserByID(user.getLongID()) != null) {
				guild.getUsers().remove(user);
				synchronized (((Guild) guild).joinTimes) {
					((Guild) guild).joinTimes.remove(user.getLongID());
				}
			}

			client.dispatcher.dispatch(new UserBanEvent(guild, user));
		}
	}

	private void guildBanRemove(GuildBanEventResponse event) {
		IGuild guild = client.getGuildByID(event.guild_id);
		if (guild != null) {
			IUser user = DiscordUtils.getUserFromJSON(shard, event.user);

			client.dispatcher.dispatch(new UserPardonEvent(guild, user));
		}
	}

	private void voiceStateUpdate(VoiceStateObject json) {
		IGuild guild = client.getGuildByID(json.getLongGuildID());

		if (guild != null) {
			IVoiceChannel channel = guild.getVoiceChannelByID(json.getLongChannelID());
			User user = (User) guild.getUserByID(json.getLongUserID());
			if (user != null) {
				user.setIsDeaf(guild.getLongID(), json.deaf);
				user.setIsMute(guild.getLongID(), json.mute);
				user.setIsDeafLocally(json.self_deaf);
				user.setIsMutedLocally(json.self_mute);

				IVoiceChannel oldChannel = user.getConnectedVoiceChannels()
						.stream()
						.filter(vChannel -> vChannel.getGuild().getLongID() == json.getLongGuildID())
						.findFirst()
						.orElse(null);
				if (oldChannel == null)
					oldChannel = user.getConnectedVoiceChannels()
							.stream()
							.findFirst()
							.orElse(null);
				if (channel != oldChannel) {
					if (channel == null) {
						client.dispatcher.dispatch(new UserVoiceChannelLeaveEvent(oldChannel, user));
						user.getConnectedVoiceChannels().remove(oldChannel);
					} else if (oldChannel != null && oldChannel.getGuild().equals(channel.getGuild())) {
						client.dispatcher.dispatch(new UserVoiceChannelMoveEvent(user, oldChannel, channel));
						user.getConnectedVoiceChannels().remove(oldChannel);
						if (!user.getConnectedVoiceChannels().contains(channel))
							user.getConnectedVoiceChannels().add(channel);
					} else {
						client.dispatcher.dispatch(new UserVoiceChannelJoinEvent(channel, user));
						if (!user.getConnectedVoiceChannels().contains(channel))
							user.getConnectedVoiceChannels().add(channel);
					}
				}
			}
		}
	}

	private void voiceServerUpdate(VoiceUpdateResponse event) {
		try {
			event.endpoint = event.endpoint.substring(0, event.endpoint.indexOf(":"));
			synchronized (client.voiceConnections) {
				client.voiceConnections.put(event.guild_id, new DiscordVoiceWS(event, shard));
			}
		} catch (Exception e) {
			Discord4J.LOGGER.error(LogMarkers.VOICE_WEBSOCKET, "Discord4J Internal Exception", e);
		}
	}

	private void guildEmojisUpdate(GuildEmojiUpdateResponse event) {
		IGuild guild = client.getGuildByID(event.guild_id);
		if (guild != null) {
			List<IEmoji> oldList = guild.getEmojis().stream().map(IEmoji::copy)
					.collect(Collectors.toCollection(CopyOnWriteArrayList::new));

			guild.getEmojis().clear();
			for (EmojiObject obj : event.emojis) {
				guild.getEmojis().add(DiscordUtils.getEmojiFromJSON(guild, obj));
			}

			client.dispatcher.dispatch(new GuildEmojisUpdateEvent(guild, oldList, guild.getEmojis()));
		}
	}

	private void reactionAdd(ReactionEventResponse event) {
		IChannel channel = client.getChannelByID(event.channel_id);
		if (channel != null) {
			IMessage message = channel.getMessageByID(event.message_id);

			if (message != null) {
				Reaction reaction = (Reaction) (event.emoji.getStringID() == null
						? message.getReactionByName(event.emoji.name)
						: message.getReactionByIEmoji(message.getGuild().getEmojiByID(event.emoji.getLongID())));
				IUser user = message.getClient().getUserByID(event.user_id);

				if (reaction == null) {
					List<IUser> list = new CopyOnWriteArrayList<>();
					list.add(user);

					reaction = new Reaction(message.getShard(), 1, list,
							event.emoji.getStringID() != null ? event.emoji.getStringID() : event.emoji.name, event.emoji.getStringID() != null);

					message.getReactions().add(reaction);
				} else {
					reaction.getCachedUsers().add(user);
					reaction.setCount(reaction.getCount() + 1);
				}

				reaction.setMessage(message);

				client.dispatcher.dispatch(
						new ReactionAddEvent(message, reaction, user));
			}
		}
	}

	private void reactionRemove(ReactionEventResponse event) {
		IChannel channel = client.getChannelByID(event.channel_id);
		if (channel != null) {
			IMessage message = channel.getMessageByID(event.message_id);

			if (message != null) {
				Reaction reaction = (Reaction) (event.emoji.getStringID() == null
						? message.getReactionByName(event.emoji.name)
						: message.getReactionByIEmoji(message.getGuild().getEmojiByID(event.emoji.getLongID())));
				IUser user = message.getClient().getUserByID(event.user_id);

				if (reaction != null) {
					reaction.setMessage(message); // safeguard
					reaction.setCount(reaction.getCount() - 1);
					reaction.getCachedUsers().remove(user);

					if (reaction.getCount() <= 0) {
						message.getReactions().remove(reaction);
					}

					client.dispatcher.dispatch(new ReactionRemoveEvent(message, reaction, user));
				}
			}
		}
	}

	private void webhookUpdate(WebhookObject event) {
		Channel channel = (Channel) client.getChannelByID(event.getLongChannelID());
		if (channel != null)
			channel.loadWebhooks();
	}
}
