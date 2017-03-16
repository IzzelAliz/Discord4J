package sx.blah.discord.api.internal.etf;

import sx.blah.discord.Discord4J;

public class IdentifyRequest {
	private final String token;
	private final Properties properties;
	private final boolean compress;
	private final int large_threshold;
	private final int[] shard;

	public IdentifyRequest(String token, int[] shard) {
		this(token, new Properties(), true, 250, shard);
	}

	private IdentifyRequest(String token, Properties properties, boolean compress, int large_threshold, int[] shard) {
		this.token = token;
		this.properties = properties;
		this.compress = compress;
		this.large_threshold = large_threshold;
		this.shard = shard;
	}

	private static class Properties {
		private final String $os;
		private final String $browser;
		private final String $device;
		private final String $referrer;
		private final String $referring_domain;

		public Properties() {
			this(System.getProperty("os.name"), Discord4J.NAME, Discord4J.NAME, "", "");
		}

		private Properties(String os, String browser, String device, String referrer, String referring_domain) {
			this.$os = os;
			this.$browser = browser;
			this.$device = device;
			this.$referrer = referrer;
			this.$referring_domain = referring_domain;
		}
	}
}
