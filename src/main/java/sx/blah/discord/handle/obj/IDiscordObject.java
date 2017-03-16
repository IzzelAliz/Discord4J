package sx.blah.discord.handle.obj;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.IShard;
import sx.blah.discord.api.internal.DiscordUtils;

import java.time.LocalDateTime;

/**
 * This represents a generic discord object.
 */
public interface IDiscordObject<SELF extends IDiscordObject> { //The SELF thing is just a hack to get copy() to work correctly because self types don't exist in java >.>

	/**
	 * Gets the snowflake unique id for this object.
	 *
	 * @return The id.
	 * @deprecated In a future release this will return long (equivalent of {@link #getLongID()}),
	 * {@link #getStringID()} is the safe replacement.
	 */
	@Deprecated
	default String getID() {
		return getStringID();
	}
	
	/**
	 * Gets the snowflake unique id for this object.
	 *
	 * @return The id.
	 */
	long getLongID();
	
	/**
	 * Gets the snowflake unique id for this object.
	 *
	 * @return The id.
	 */
	default String getStringID() {
		return Long.toUnsignedString(getLongID());
	}

	/**
	 * Gets the {@link IDiscordClient} instance this object belongs to.
	 *
	 * @return The client instance.
	 */
	IDiscordClient getClient();

	/**
	 * Get the {@link IShard} instance this object belongs to.
	 */
	IShard getShard();

	/**
	 * Gets the {@link LocalDateTime} this object was created at. This is calculated by reversing the snowflake
	 * algorithm on the object's id.
	 *
	 * @return The creation date of this object.
	 */
	default LocalDateTime getCreationDate() {
		return DiscordUtils.getSnowflakeTimeFromID(getLongID());
	}

	/**
	 * Creates a new instance of this object with all the current properties.
	 *
	 * @return The copied instance of this object.
	 */
	SELF copy();
}
