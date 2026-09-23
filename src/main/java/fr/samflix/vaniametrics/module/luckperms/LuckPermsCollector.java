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
 * The LuckPerms collector.
 *
 * <p>Backgrounded, for a single metric: {@code getUniqueUsers()} scans the whole
 * user table. Groups are already in memory and cost nothing — but splitting
 * would mean two collectors for one subject, and being five minutes behind on
 * an account count never changed a decision.
 *
 * <p>Group membership only counts online players, and that's deliberate: the
 * API has no way to count a group's members without loading every user, one
 * by one, from the database. The overall count is a single SQL query on
 * {@code luckperms_user_permissions} — that's the sql module's job, not this
 * one's.
 */
public final class LuckPermsCollector implements Collector {

	private final Platform platform;

	private Gauge groups;
	private Gauge tracks;
	private Gauge users;
	private Gauge onlineByGroup;
	private Gauge groupWeight;

	public LuckPermsCollector(Platform platform) {
		this.platform = platform;
	}

	@Override
	public String name() {
		return "luckperms";
	}

	@Override
	public String source() {
		return "LuckPerms";
	}

	@Override
	public boolean isBackground() {
		return true;
	}

	@Override
	public long intervalSeconds() {
		return 300;
	}

	@Override
	public void declare(MetricRegistry r) {
		groups = r.gauge("permission_groups", "Declared groups.");
		tracks = r.gauge("permission_tracks", "Declared promotion tracks.");
		users = r.gauge("permission_users",
				"Users known to LuckPerms. Scans the table: collected in the background.");
		onlineByGroup = r.gauge("permission_online_by_group",
				"Players ONLINE by primary group. For the offline total, see the "
						+ "sql module — the API can't count without loading everything.",
				"group");
		groupWeight = r.gauge("permission_group_weight",
				"A group's weight, as LuckPerms uses it to resolve conflicts.",
				"group");
	}

	@Override
	public void collect(MetricRegistry r) throws Exception {
		LuckPerms lp = LuckPermsProvider.get();

		groups.set(lp.getGroupManager().getLoadedGroups().size());
		tracks.set(lp.getTrackManager().getLoadedTracks().size());

		groupWeight.clear();
		for (Group g : lp.getGroupManager().getLoadedGroups()) {
			g.getWeight().ifPresent(p -> groupWeight.set(p, g.getName()));
		}

		onlineByGroup.clear();
		Map<String, Integer> count = new HashMap<>();
		for (User u : lp.getUserManager().getLoadedUsers()) {
			count.merge(u.getPrimaryGroup(), 1, Integer::sum);
		}
		count.forEach((group, n) -> onlineByGroup.set(n, group));

		// The one expensive call, and it's asynchronous by construction. The
		// timeout bounds it: a slow database shouldn't hold the background task
		// past the next collection.
		users.set(lp.getUserManager().getUniqueUsers().get(20, TimeUnit.SECONDS).size());
	}
}
