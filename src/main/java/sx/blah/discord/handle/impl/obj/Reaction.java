package sx.blah.discord.handle.impl.obj;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import org.apache.http.message.BasicNameValuePair;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.IShard;
import sx.blah.discord.api.internal.DiscordClientImpl;
import sx.blah.discord.api.internal.DiscordEndpoints;
import sx.blah.discord.api.internal.json.objects.ReactionUserObject;
import sx.blah.discord.handle.obj.IEmoji;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IReaction;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RateLimitException;

import java.util.List;

public class Reaction implements IReaction {

	protected final IShard shard;
	protected final boolean isCustomEmoji;
	protected final String emoji;
	/**
	 * How many users reacted
	 */
	protected volatile int count;
	/**
	 * The users that reacted
	 */
	protected volatile List<IUser> users;
	/**
	 * Can't be final since it has to be set after creation
	 */
	private volatile IMessage message;

	public Reaction(IShard shard, int count, List<IUser> users, String emoji, boolean isCustom) {
		this.shard = shard;
		this.count = count;
		this.users = users;
		this.emoji = emoji;
		this.isCustomEmoji = isCustom;
	}

	@Override
	public String toString() {
		return isCustomEmoji() ? getCustomEmoji().toString() : emoji;
	}

	@Override
	public boolean getUserReacted(IUser user) {
		return users.stream().anyMatch(u -> u.equals(user));
	}

	@Override
	public boolean getClientReacted() {
		return getUserReacted(getClient().getOurUser());
	}

	@Override
	public IMessage getMessage() {
		return message;
	}

	public void setMessage(IMessage message) {
		this.message = message;
	}

	@Override
	public boolean isCustomEmoji() {
		return isCustomEmoji;
	}

	@Override
	public IEmoji getCustomEmoji() {
		return getMessage().getGuild().getEmojiByID(emoji);
	}

	@Override
	public Emoji getUnicodeEmoji() {
		return EmojiManager.getByUnicode(emoji);
	}

	@Override
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * Returns the CACHED users.
	 *
	 * @return The CACHED users
	 */
	public List<IUser> getCachedUsers() {
		return users;
	}

	@Override
	public synchronized List<IUser> getUsers() throws DiscordException, RateLimitException {
		if (shouldRefreshUsers()) {
			users.clear();

			int gottenSoFar = 0;
			String emoji = isCustomEmoji() ? (getCustomEmoji().getName() + ":" + getCustomEmoji().getStringID()) : this.emoji;
			Long userAfter = null;
			while (gottenSoFar < count) {
				ReactionUserObject[] userObjs = ((DiscordClientImpl) getClient()).REQUESTS.GET.makeRequest(
						String.format(DiscordEndpoints.REACTIONS_USER_LIST, message.getChannel().getStringID(), message.getStringID(), emoji),
						ReactionUserObject[].class,
						new BasicNameValuePair("after", userAfter == null ? null : Long.toUnsignedString(userAfter)));

				if (userObjs.length == 0)
					break;

				gottenSoFar += userObjs.length;

				for (ReactionUserObject obj : userObjs) {
					IUser u = getClient().getUserByID(obj.getLongID());

					if (u != null) {
						users.add(u);
					}

					userAfter = obj.getLongID();
				}
			}
		}

		return users;
	}

	private boolean shouldRefreshUsers() {
		return users.size() != count;
	}

	@Override
	public IDiscordClient getClient() {
		return shard.getClient();
	}

	@Override
	public IShard getShard() {
		return shard;
	}

	@Override
	public IReaction copy() {
		return new Reaction(shard, count, users, emoji, isCustomEmoji);
	}
}
