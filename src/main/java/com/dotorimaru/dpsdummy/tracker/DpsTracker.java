package com.dotorimaru.dpsdummy.tracker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 공격자별 롤링 윈도우 DPS 추적 + 액션바 출력.
 *
 * 갱신은 타격 이벤트가 아니라 2틱 주기 반복 태스크로 처리한다.
 * (공격을 멈춰도 DPS가 실시간으로 떨어지는 게 보이도록)
 * 윈도우가 완전히 비면(무타격) 세션 종료 메시지를 띄우고 맵에서 제거한다.
 */
public class DpsTracker {

    private record Hit(long time, double damage) {
    }

    private static final class Session {
        final Deque<Hit> hits = new ArrayDeque<>();
        double sum = 0.0;
        double peak = 0.0;
        boolean armored = false;
    }

    private final JavaPlugin plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private long windowMillis;
    private BukkitTask task;

    public DpsTracker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.windowMillis = Math.max(1, plugin.getConfig().getInt("window-seconds", 5)) * 1000L;
    }

    /** 2틱(0.1초) 주기 액션바 갱신 태스크 시작 */
    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        sessions.clear();
    }

    /** 타격 기록 (메인 스레드에서만 호출) */
    public void record(Player attacker, double damage, boolean armored) {
        Session session = sessions.computeIfAbsent(attacker.getUniqueId(), key -> new Session());
        session.hits.addLast(new Hit(System.currentTimeMillis(), damage));
        session.sum += damage;
        session.armored = armored;
    }

    private void tick() {
        if (sessions.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        double windowSeconds = windowMillis / 1000.0;

        Iterator<Map.Entry<UUID, Session>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Session> entry = iterator.next();
            Session session = entry.getValue();

            // 윈도우 밖으로 밀려난 기록 제거 (합계는 증분 유지 — 매 틱 전체 순회 방지)
            while (!session.hits.isEmpty() && now - session.hits.peekFirst().time() > windowMillis) {
                session.sum -= session.hits.pollFirst().damage();
            }

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            // 윈도우 동안 무타격 → 세션 종료
            if (session.hits.isEmpty()) {
                player.sendActionBar(Component.text(
                        "측정 종료 — 최고 DPS: %.1f".formatted(session.peak), NamedTextColor.GRAY));
                iterator.remove();
                continue;
            }

            double dps = Math.max(0.0, session.sum) / windowSeconds;
            if (dps > session.peak) {
                session.peak = dps;
            }

            Component bar = Component.text("⚔ DPS: ", NamedTextColor.YELLOW)
                    .append(Component.text("%.1f".formatted(dps), NamedTextColor.WHITE))
                    .append(Component.text(" (%.0fs)".formatted(windowSeconds), NamedTextColor.DARK_GRAY));
            if (session.armored) {
                bar = bar.append(Component.text(" 🛡", NamedTextColor.AQUA));
            }
            player.sendActionBar(bar);
        }
    }
}
