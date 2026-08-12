package com.dotorimaru.dpsdummy.tracker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

    /** DPS 구간별 색상: 50 미만 노랑/흰색, 50 이상 초록→하늘색, 100 이상 빨강→주황 그라데이션 */
    private static final double MID_THRESHOLD = 50.0;
    private static final double HIGH_THRESHOLD = 100.0;
    private static final TextColor MID_FROM = NamedTextColor.GREEN;   // #55FF55
    private static final TextColor MID_TO = NamedTextColor.AQUA;      // #55FFFF (하늘색)
    private static final TextColor HIGH_FROM = NamedTextColor.RED;    // #FF5555
    private static final TextColor HIGH_TO = TextColor.color(0xFFAA00); // 주황

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

            // 윈도우 동안 무타격 → 세션 종료 (최고 DPS도 구간 색상 적용)
            if (session.hits.isEmpty()) {
                Component result = Component.text("측정 종료 — ", NamedTextColor.GRAY)
                        .append(colored("최고 DPS: %.1f".formatted(session.peak), session.peak, NamedTextColor.GRAY));
                player.sendActionBar(result.decorate(TextDecoration.BOLD));
                iterator.remove();
                continue;
            }

            double dps = Math.max(0.0, session.sum) / windowSeconds;
            if (dps > session.peak) {
                session.peak = dps;
            }

            Component bar = colored("⚔ DPS: %.1f".formatted(dps), dps, null)
                    .append(Component.text(" (%.0fs)".formatted(windowSeconds), NamedTextColor.DARK_GRAY));
            if (session.armored) {
                bar = bar.append(Component.text(" 🛡", NamedTextColor.AQUA));
            }
            player.sendActionBar(bar.decorate(TextDecoration.BOLD));
        }
    }

    /**
     * DPS 구간별 텍스트 색상.
     * 50 이상: 초록→하늘색, 100 이상: 빨강→주황 그라데이션.
     * 그 미만: fallback 색(null이면 기존 노랑 라벨 + 흰색 수치 스타일).
     */
    private static Component colored(String text, double dps, TextColor fallback) {
        if (dps >= HIGH_THRESHOLD) {
            return gradient(text, HIGH_FROM, HIGH_TO);
        }
        if (dps >= MID_THRESHOLD) {
            return gradient(text, MID_FROM, MID_TO);
        }
        if (fallback != null) {
            return Component.text(text, fallback);
        }
        int split = text.lastIndexOf(' ') + 1;
        return Component.text(text.substring(0, split), NamedTextColor.YELLOW)
                .append(Component.text(text.substring(split), NamedTextColor.WHITE));
    }

    /** 글자 단위 색 보간으로 그라데이션 텍스트 생성 */
    private static Component gradient(String text, TextColor from, TextColor to) {
        var builder = Component.text();
        int[] codePoints = text.codePoints().toArray();
        int last = codePoints.length - 1;
        for (int i = 0; i <= last; i++) {
            float t = last == 0 ? 0.0f : (float) i / last;
            builder.append(Component.text(
                    new String(Character.toChars(codePoints[i])), TextColor.lerp(t, from, to)));
        }
        return builder.build();
    }
}
