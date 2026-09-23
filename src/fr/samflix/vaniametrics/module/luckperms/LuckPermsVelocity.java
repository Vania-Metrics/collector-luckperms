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
 * LuckPerms — groupes, pistes, et où se répartissent les joueurs. — côté Velocity.
 *
 * <p>Même collecteur, autre point d'entrée. Les deux classes cohabitent dans le MÊME jar : Bukkit
 * lit plugin.yml et charge la variante Paper, Velocity lit velocity-plugin.json et charge
 * celle-ci. Chacun ignore l'autre, qui n'est jamais chargée.
 */
@Plugin(
		id = "vaniametrics-luckperms",
		name = "VaniaMetrics LuckPerms",
		version = Version.VALEUR,
		description = "LuckPerms — groupes, pistes, et où se répartissent les joueurs.",
		authors = {"mc-vania"},
		dependencies = {
			@Dependency(id = "vaniametrics"),
			@Dependency(id = "luckperms")
		})
public final class LuckPermsVelocity {

	private final Logger journal;
	private Collector collecteur;

	@Inject
	public LuckPermsVelocity(Logger journal) {
		this.journal = journal;
	}

	@Subscribe
	public void onInit(ProxyInitializeEvent e) {
		VaniaMetrics metriques = VaniaMetricsProvider.get();
		collecteur = new LuckPermsCollector(metriques.plateforme());
		metriques.enregistrer(collecteur);
		journal.info("collecteur luckperms enregistré");
	}

	@Subscribe
	public void onShutdown(ProxyShutdownEvent e) {
		if (collecteur != null) {
			VaniaMetricsProvider.chercher().ifPresent(m -> m.retirer(collecteur));
		}
	}
}
