package fr.samflix.vaniametrics.module.luckperms;

import org.bukkit.plugin.java.JavaPlugin;

import fr.samflix.vaniametrics.api.Collector;
import fr.samflix.vaniametrics.api.VaniaMetrics;
import fr.samflix.vaniametrics.api.VaniaMetricsProvider;

/**
 * LuckPerms — groupes, pistes, et où se répartissent les joueurs.
 *
 * <p>Sur les deux plateformes : son API est la même, et un proxy qui porterait LuckPerms un jour publierait les mêmes chiffres sans qu'on change une ligne.
 *
 * <p>SON plugin.yml DÉCLARE {@code depend: [VaniaMetrics, LuckPerms]} : les deux sont
 * indispensables, et le déclarer laisse Bukkit garantir l'ordre de chargement plutôt que de
 * l'espérer. Retirer ce jar retire cette intégration et RIEN D'AUTRE — c'est tout l'intérêt d'un
 * jar par intégration.
 */
public final class LuckPermsPaper extends JavaPlugin {

	private Collector collecteur;

	@Override
	public void onEnable() {
		VaniaMetrics metriques = VaniaMetricsProvider.get();
		collecteur = new LuckPermsCollector(metriques.plateforme());
		metriques.enregistrer(collecteur);
	}

	@Override
	public void onDisable() {
		if (collecteur != null) {
			VaniaMetricsProvider.chercher().ifPresent(m -> m.retirer(collecteur));
		}
	}
}
