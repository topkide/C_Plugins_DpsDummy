package com.dotorimaru.dpsdummy.dummy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * DPS 측정기(갑옷 거치대)의 소환 / 식별 / 제거 담당.
 * PDC 태그 기반 식별이라 일반 장식용 갑옷 거치대는 절대 건드리지 않는다.
 */
public class DummyManager {

    /** 방어구 4부위 슬롯 (장비 드롭/방어 계산에 공용) */
    public static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final NamespacedKey dummyKey;

    public DummyManager(JavaPlugin plugin) {
        this.dummyKey = new NamespacedKey(plugin, "dps_dummy");
    }

    /** 플레이어 위치에 측정기 소환 (플레이어를 마주보도록 회전) */
    public ArmorStand spawn(Player player) {
        Location loc = player.getLocation().clone();
        loc.setYaw(loc.getYaw() + 180.0f);
        loc.setPitch(0.0f);

        return player.getWorld().spawn(loc, ArmorStand.class, stand -> {
            stand.getPersistentDataContainer().set(dummyKey, PersistentDataType.BYTE, (byte) 1);
            stand.setGravity(false);
            stand.setPersistent(true);
            stand.setArms(true);
            stand.setBasePlate(false);
            stand.customName(Component.text("DPS 측정기", NamedTextColor.GOLD));
            stand.setCustomNameVisible(true);
        });
    }

    /** 이 엔티티가 DPS 측정기인지 (PDC 태그 확인) */
    public boolean isDummy(Entity entity) {
        return entity instanceof ArmorStand
                && entity.getPersistentDataContainer().has(dummyKey, PersistentDataType.BYTE);
    }

    /** 반경 내 측정기 전부 제거. 장착 장비는 바닥에 드롭. 제거된 개수 반환. */
    public int removeNearby(Player player, double radius) {
        int removed = 0;
        for (ArmorStand stand : player.getLocation().getNearbyEntitiesByType(ArmorStand.class, radius)) {
            if (!isDummy(stand)) {
                continue;
            }
            dropEquipment(stand);
            stand.remove();
            removed++;
        }
        return removed;
    }

    private void dropEquipment(ArmorStand stand) {
        EntityEquipment equipment = stand.getEquipment();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack item = equipment.getItem(slot);
            if (!item.getType().isAir()) {
                stand.getWorld().dropItemNaturally(stand.getLocation(), item);
            }
        }
    }
}
