package fr.samflix.vaniametrics.module.luckperms;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;

import fr.samflix.vaniametrics.api.Collector;
import fr.samflix.vaniametrics.api.Gauge;
import fr.samflix.vaniametrics.api.MetricRegistry;
import fr.samflix.vaniametrics.api.Platform;

/**
 * Le relevé LuckPerms.
 *
 * <p>EN FOND, ET POUR UNE SEULE MÉTRIQUE : {@code getUniqueUsers()} balaie toute la table des
 * joueurs. Les groupes, eux, sont déjà en mémoire et ne coûtent rien — mais séparer ferait deux
 * collecteurs pour un même sujet, et cinq minutes de retard sur un nombre de comptes n'a jamais
 * changé une décision.
 *
 * <p>ON NE COMPTE LES MEMBRES D'UN GROUPE QUE PARMI LES JOUEURS EN LIGNE, et c'est délibéré :
 * l'API n'a aucun moyen de compter les membres d'un groupe sans charger TOUS les utilisateurs, un
 * par un, depuis la base. Le chiffre global se lit en une requête SQL sur
 * {@code luckperms_user_permissions} — c'est le travail du module SQL, pas de celui-ci.
 */
public final class LuckPermsCollector implements Collector {

	private final Platform plateforme;

	private Gauge groupes;
	private Gauge pistes;
	private Gauge comptes;
	private Gauge enLigneParGroupe;
	private Gauge poidsGroupe;

	public LuckPermsCollector(Platform plateforme) {
		this.plateforme = plateforme;
	}

	@Override
	public String nom() {
		return "luckperms";
	}

	@Override
	public String origine() {
		return "LuckPerms";
	}

	@Override
	public boolean enFond() {
		return true;
	}

	@Override
	public long intervalleSecondes() {
		return 300;
	}

	@Override
	public void declarer(MetricRegistry r) {
		groupes = r.gauge("permission_groups", "Groupes déclarés.");
		pistes = r.gauge("permission_tracks", "Pistes de promotion déclarées.");
		comptes = r.gauge("permission_users",
				"Comptes connus de LuckPerms. Balaie la table : relevé en fond.");
		enLigneParGroupe = r.gauge("permission_online_by_group",
				"Joueurs EN LIGNE par groupe principal. Pour le total hors ligne, voir le module "
						+ "sql — l'API ne sait pas compter sans tout charger.",
				"group");
		poidsGroupe = r.gauge("permission_group_weight",
				"Poids d'un groupe, tel que LuckPerms l'utilise pour trancher les conflits.",
				"group");
	}

	@Override
	public void relever(MetricRegistry r) throws Exception {
		LuckPerms lp = LuckPermsProvider.get();

		groupes.set(lp.getGroupManager().getLoadedGroups().size());
		pistes.set(lp.getTrackManager().getLoadedTracks().size());

		poidsGroupe.clear();
		for (Group g : lp.getGroupManager().getLoadedGroups()) {
			g.getWeight().ifPresent(p -> poidsGroupe.set(p, g.getName()));
		}

		enLigneParGroupe.clear();
		Map<String, Integer> compte = new HashMap<>();
		for (User u : lp.getUserManager().getLoadedUsers()) {
			compte.merge(u.getPrimaryGroup(), 1, Integer::sum);
		}
		compte.forEach((groupe, n) -> enLigneParGroupe.set(n, groupe));

		// Le seul appel coûteux, et il est asynchrone par construction. Le délai le borne : une
		// base lente ne doit pas retenir la tâche de fond jusqu'au relevé suivant.
		comptes.set(lp.getUserManager().getUniqueUsers().get(20, TimeUnit.SECONDS).size());
	}
}
