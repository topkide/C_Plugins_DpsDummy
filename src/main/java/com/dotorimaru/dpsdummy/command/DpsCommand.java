package com.dotorimaru.dpsdummy.command;

import com.dotorimaru.dpsdummy.dummy.DummyManager;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /dps — 현재 위치에 DPS 측정기 소환
 */
@RequiredArgsConstructor
public class DpsCommand implements TabExecutor {

    private final DummyManager dummyManager;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용할 수 있는 명령어입니다.", NamedTextColor.RED));
            return true;
        }

        dummyManager.spawn(player);
        player.sendMessage(Component.text("DPS 측정기를 소환했습니다. ", NamedTextColor.GOLD)
                .append(Component.text("때리면 액션바에 실시간 DPS가 표시됩니다. 갑옷/머리를 들고 우클릭하면 장착할 수 있습니다.",
                        NamedTextColor.GRAY)));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, String @NotNull [] args) {
        return List.of();
    }
}
