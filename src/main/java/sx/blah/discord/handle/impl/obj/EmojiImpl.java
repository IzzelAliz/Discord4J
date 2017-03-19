package sx.blah.discord.handle.impl.obj;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.IShard;
import sx.blah.discord.api.internal.DiscordEndpoints;
import sx.blah.discord.handle.obj.IEmoji;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EmojiImpl implements IEmoji {

	/**
	 * The guild.
	 */
	protected final IGuild guild;
	/**
	 * The ID.
	 */
	protected final long id;
	/**
	 * The name.
	 */
	protected volatile String name;
	/**
	 * If it requires colons :X:
	 */
	protected volatile boolean requiresColons;
	/**
	 * If it's managed externally.
	 */
	protected volatile boolean isManaged;

	public EmojiImpl(IGuild guild, long id, String name, boolean requiresColons, boolean isManaged) {
		this.guild = guild;
		this.id = id;
		this.name = name;
		this.requiresColons = requiresColons;
		this.isManaged = isManaged;
	}

	public void setRequiresColons(boolean requiresColons) {
		this.requiresColons = requiresColons;
	}
	
	@Override
	public long getLongID() {
		return id;
	}
	
	@Override
	public IDiscordClient getClient() {
		return getGuild().getClient();
	}

	@Override
	public IShard getShard() {
		return getGuild().getShard();
	}

	@Override
	public synchronized IEmoji copy() {
		EmojiImpl copy = new EmojiImpl(guild, id, name, requiresColons, isManaged);

		return copy;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public IGuild getGuild() {
		return guild;
	}

	@Override
	public boolean requiresColons() {
		return requiresColons;
	}

	@Override
	public boolean isManaged() {
		return isManaged;
	}

	public void setManaged(boolean managed) {
		isManaged = managed;
	}

	@Override
	public List<IRole> getRoles() {
		return new ArrayList<>();
	}

	@Override
	public String getImageUrl() {
		return String.format(DiscordEndpoints.EMOJI_IMAGE, getID());
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;

		return this.getClass().isAssignableFrom(other.getClass()) && ((IEmoji) other).getLongID() == getLongID();
	}

	@Override
	public String toString() {
		return "<:" + getName() + ":" + getStringID() + ">";
	}
}
