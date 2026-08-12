package com.dotorimaru.dpsdummy.command;

import com.dotorimaru.dpsdummy.dummy.DummyManager;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /undps — 반경 내 DPS 측정기 제거 (장착 장비는 바닥에 드롭)
 */
@RequiredArgsConstructor
public class UndpsCommand implements TabExecutor {

    private final JavaPlugin plugin;
    private final DummyManager dummyManager;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용할 수 있는 명령어입니다.", NamedTextColor.RED));
            return true;
        }

        double radius = plugin.getConfig().getDouble("remove-radius", 5.0);
        int removed = dummyManager.removeNearby(player, radius);

        if (removed == 0) {
            player.sendMessage(Component.text(
                    "반경 %.0f블록 내에 DPS 측정기가 없습니다.".formatted(radius), NamedTextColor.RED));
        } else {
            player.sendMessage(Component.text(
                    "DPS 측정기 %d개를 제거했습니다. 장착돼 있던 장비는 바닥에 드롭됐습니다.".formatted(removed),
                    NamedTextColor.GOLD));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, String @NotNull [] args) {
        return List.of();
    }
}
