package sx.blah.discord.handle.impl.obj;

import sx.blah.discord.api.internal.DiscordClientImpl;
import sx.blah.discord.api.internal.DiscordEndpoints;
import sx.blah.discord.api.internal.json.objects.InviteObject;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IInvite;
import sx.blah.discord.util.RateLimitException;

import java.util.Objects;

public class Invite implements IInvite {
	/**
	 * An invite code, AKA an invite URL minus the https://discord.gg/
	 */
	protected final String inviteCode;

	/**
	 * The client that created this object.
	 */
	protected final IDiscordClient client;

	public Invite(IDiscordClient client, String inviteCode) {
		this.client = client;
		this.inviteCode = inviteCode;
	}

	@Override
	public String getInviteCode() {
		return inviteCode;
	}

	@Override
	public InviteResponse details() throws DiscordException, RateLimitException {
		client.checkReady("get invite details");
		InviteObject response = ((DiscordClientImpl) client).REQUESTS.GET.makeRequest(DiscordEndpoints.INVITE+inviteCode, InviteObject.class);

		return new InviteResponse(response.guild.getLongID(), response.guild.name, response.channel.getLongID(), response.channel.name);
	}

	@Override
	public void delete() throws DiscordException, RateLimitException {
		((DiscordClientImpl) client).REQUESTS.DELETE.makeRequest(DiscordEndpoints.INVITE+inviteCode);
	}

	@Override
	public IDiscordClient getClient() {
		return client;
	}

	@Override
	public int hashCode() {
		return Objects.hash(inviteCode);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;

		return this.getClass().isAssignableFrom(other.getClass()) && ((IInvite) other).getInviteCode().equals(getInviteCode());
	}

	@Override
	public String toString() {
		return inviteCode;
	}
}
