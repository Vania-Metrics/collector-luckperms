package fr.samflix.vaniametrics.module.luckperms;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;

import fr.samflix.vaniametrics.api.Collector;
import fr.samflix.vaniametrics.api.VaniaMetrics;
import fr.samflix.vaniametrics.api.VaniaMetricsProvider;
import fr.samflix.vaniametrics.api.Version;

/**
 * LuckPerms — groups, tracks, and where players fall. — Velocity side.
 *
 * <p>Same collector, different entry point. All three classes live in the SAME
 * jar: Bukkit reads plugin.yml and loads the Paper variant, BungeeCord reads
 * bungee.yml and loads the Bungee one, Velocity reads velocity-plugin.json and
 * loads this one. Each ignores the others, which are never loaded.
 */
@Plugin(
		id = "vaniametrics-luckperms",
		name = "VaniaMetrics LuckPerms",
		version = Version.VALUE,
		description = "LuckPerms — groups, tracks, and where players fall.",
		authors = {"mc-vania"},
		dependencies = {
			@Dependency(id = "vaniametrics"),
			@Dependency(id = "luckperms")
		})
public final class LuckPermsVelocity {

	private final Logger logger;
	private Collector collector;

	@Inject
	public LuckPermsVelocity(Logger logger) {
		this.logger = logger;
	}

	@Subscribe
	public void onInit(ProxyInitializeEvent e) {
		VaniaMetrics metrics = VaniaMetricsProvider.get();
		collector = new LuckPermsCollector(metrics.platform());
		metrics.register(collector);
		logger.info("luckperms collector registered");
	}

	@Subscribe
	public void onShutdown(ProxyShutdownEvent e) {
		if (collector != null) {
			VaniaMetricsProvider.find().ifPresent(m -> m.unregister(collector));
		}
	}
}
