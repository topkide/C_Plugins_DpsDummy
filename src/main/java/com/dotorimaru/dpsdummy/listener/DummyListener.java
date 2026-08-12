package com.dotorimaru.dpsdummy.listener;

import com.dotorimaru.dpsdummy.combat.DamageCalculator;
import com.dotorimaru.dpsdummy.dummy.DummyManager;
import com.dotorimaru.dpsdummy.tracker.DpsTracker;
import lombok.RequiredArgsConstructor;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EquipmentSlot;

@RequiredArgsConstructor
public class DummyListener implements Listener {

    private final DummyManager dummyManager;
    private final DpsTracker dpsTracker;

    /**
     * 측정기 타격 처리.
     * setInvulnerable 대신 이벤트 캔슬로 무적을 구현한다 —
     * invulnerable 엔티티는 데미지 이벤트 자체가 안 뜨는 케이스가 있어 측정이 불가능하기 때문.
     * 폭발/화염 등 환경 데미지도 전부 캔슬만 하고 기록하지 않는다.
     */
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!dummyManager.isDummy(event.getEntity())) {
            return;
        }
        event.setCancelled(true);

        if (!(event instanceof EntityDamageByEntityEvent byEntity)) {
            return;
        }
        Player attacker = resolveAttacker(byEntity.getDamager());
        if (attacker == null) {
            return;
        }

        ArmorStand dummy = (ArmorStand) event.getEntity();

        // 원본 데미지(크리/힘/날카로움 포함)에 방어구 감소를 직접 계산해서 기록
        double damage = DamageCalculator.apply(dummy, event.getDamage());
        dpsTracker.record(attacker, damage, DamageCalculator.hasArmor(dummy));

        // 캔슬로 사라진 타격 피드백 복원
        dummy.getWorld().playSound(dummy.getLocation(), Sound.ENTITY_ARMOR_STAND_HIT, 1.0f, 1.0f);
    }

    /** 화살 등 투사체는 쏜 플레이어에게 귀속 (원거리 DPS 측정) */
    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    /**
     * 장비 상호작용 필터.
     * 손 슬롯만 캔슬하고 나머지는 바닐라에 위임 —
     * 갑옷/머리 장착, 빈손 우클릭 회수가 전부 바닐라 동작으로 공짜 처리된다.
     */
    @EventHandler(ignoreCancelled = true)
    public void onManipulate(PlayerArmorStandManipulateEvent event) {
        if (!dummyManager.isDummy(event.getRightClicked())) {
            return;
        }
        EquipmentSlot slot = event.getSlot();
        if (slot == EquipmentSlot.HAND || slot == EquipmentSlot.OFF_HAND) {
            event.setCancelled(true);
        }
    }
}
