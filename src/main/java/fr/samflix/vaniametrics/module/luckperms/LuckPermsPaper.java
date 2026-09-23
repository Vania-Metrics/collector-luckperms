package fr.samflix.vaniametrics.module.luckperms;

import org.bukkit.plugin.java.JavaPlugin;

import fr.samflix.vaniametrics.api.Collector;
import fr.samflix.vaniametrics.api.VaniaMetrics;
import fr.samflix.vaniametrics.api.VaniaMetricsProvider;

/**
 * LuckPerms — groups, tracks, and where players fall.
 *
 * <p>Same API on both platforms: if a proxy ever ran LuckPerms too, it would
 * publish the same numbers without changing a line here.
 *
 * <p>Its plugin.yml declares {@code depend: [VaniaMetrics, LuckPerms]}: both
 * are required, and declaring it lets Bukkit guarantee load order instead of
 * hoping for it. Removing this jar removes this integration and nothing else
 * — that's the whole point of one jar per integration.
 */
public final class LuckPermsPaper extends JavaPlugin {

	private Collector collector;

	@Override
	public void onEnable() {
		VaniaMetrics metrics = VaniaMetricsProvider.get();
		collector = new LuckPermsCollector(metrics.platform());
		metrics.register(collector);
	}

	@Override
	public void onDisable() {
		if (collector != null) {
			VaniaMetricsProvider.find().ifPresent(m -> m.unregister(collector));
		}
	}
}
