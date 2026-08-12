package com.dotorimaru.dpsdummy;

import com.dotorimaru.dpsdummy.command.DpsCommand;
import com.dotorimaru.dpsdummy.command.UndpsCommand;
import com.dotorimaru.dpsdummy.dummy.DummyManager;
import com.dotorimaru.dpsdummy.listener.DummyListener;
import com.dotorimaru.dpsdummy.tracker.DpsTracker;
import lombok.Getter;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DpsDummyPlugin extends JavaPlugin {

    @Getter
    private static DpsDummyPlugin instance;
    @Getter
    private DummyManager dummyManager;
    @Getter
    private DpsTracker dpsTracker;

    @Override
    public void onEnable() {
        // 인스턴스
        instance = this;

        // 콘피그
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);

        // 매니저
        dummyManager = new DummyManager(this);
        dpsTracker = new DpsTracker(this);

        // 액션바 갱신 태스크 상시 가동 (측정 중인 플레이어가 없으면 비용 0)
        dpsTracker.start();

        // 리스너
        getServer().getPluginManager().registerEvents(
                new DummyListener(dummyManager, dpsTracker), this);

        // 명령어
        DpsCommand dpsCommand = new DpsCommand(dummyManager);
        PluginCommand dps = getCommand("dps");
        if (dps != null) {
            dps.setExecutor(dpsCommand);
            dps.setTabCompleter(dpsCommand);
        }

        UndpsCommand undpsCommand = new UndpsCommand(this, dummyManager);
        PluginCommand undps = getCommand("undps");
        if (undps != null) {
            undps.setExecutor(undpsCommand);
            undps.setTabCompleter(undpsCommand);
        }

        getLogger().info("DpsDummy 플러그인이 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        if (dpsTracker != null) {
            dpsTracker.stop();
        }
        getLogger().info("DpsDummy 플러그인이 종료되었습니다.");
    }
}
