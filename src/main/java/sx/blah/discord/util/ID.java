package sx.blah.discord.util;

import java.util.Arrays;

/**
 * Simple wrapper for handling string/long id conversions.
 */
public class ID {
	
	private long id;
	
	public ID(String id) {
		if (id == null)
			this.id = 0;
		else
			this.id = Long.parseUnsignedLong(id);
	}
	
	public ID(long id) {
		this.id = id;
	}
	
	public String getStringID() {
		return id == 0 ? null : Long.toUnsignedString(id);
	}
	
	public void setStringID(String id) {
		if (id == null)
			this.id = 0;
		else
			this.id = Long.parseUnsignedLong(id);
	}
	
	public long getLongID() {
		return id;
	}
	
	public void setLongID(long id) {
		this.id = id;
	}
	
	@Override
	public String toString() {
		return getStringID();
	}
	
	public static ID[] of(String[] strings) {
		if (strings == null)
			return null;
		
		return (ID[]) Arrays.stream(strings).map(ID::new).toArray();
	}
	
	public static String[] toStrings(ID[] ids) {
		if (ids == null)
			return null;
		
		return (String[]) Arrays.stream(ids).map(ID::getStringID).toArray();
	}
	
	public static ID[] of(long[] longs) {
		if (longs == null)
			return null;
		
		return (ID[]) Arrays.stream(longs).mapToObj(ID::new).toArray();
	}
	
	public static long[] toLongs(ID[] ids) {
		if (ids == null)
			return null;
		
		return Arrays.stream(ids).mapToLong(ID::getLongID).toArray();
	}
}
