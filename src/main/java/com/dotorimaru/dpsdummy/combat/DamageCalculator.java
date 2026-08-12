package com.dotorimaru.dpsdummy.combat;

import com.dotorimaru.dpsdummy.dummy.DummyManager;
import com.google.common.collect.Multimap;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 갑옷 거치대는 바닐라에서 방어구 데미지 감소를 받지 않으므로,
 * 바닐라 1.9+ 방어 공식을 직접 계산한다.
 *
 * 방어구 수치는 하드코딩 테이블 대신 아이템의 기본 어트리뷰트에서 읽으므로
 * 사슬/네더라이트/커스텀 방어구 모두 자동 대응된다.
 */
public final class DamageCalculator {

    private DamageCalculator() {
    }

    /**
     * 원본 데미지(크리/힘/날카로움 포함)에 방어 포인트·강도·보호 인챈트를 적용한 값 반환.
     */
    public static double apply(ArmorStand dummy, double baseDamage) {
        double defense = 0.0;
        double toughness = 0.0;
        int epf = 0;

        EntityEquipment equipment = dummy.getEquipment();
        for (EquipmentSlot slot : DummyManager.ARMOR_SLOTS) {
            ItemStack item = equipment.getItem(slot);
            if (item.getType().isAir()) {
                continue;
            }

            // 커스텀 방어구 대응: 아이템에 어트리뷰트가 직접 지정돼 있으면(바닐라 규칙대로)
            // 기본값을 대체하고, 없으면 재질 기본 어트리뷰트를 사용한다.
            ItemMeta meta = item.getItemMeta();
            Multimap<Attribute, AttributeModifier> modifiers =
                    meta != null && meta.hasAttributeModifiers()
                            ? meta.getAttributeModifiers(slot)
                            : item.getType().getDefaultAttributeModifiers(slot);
            for (AttributeModifier modifier : modifiers.get(Attribute.ARMOR)) {
                defense += modifier.getAmount();
            }
            for (AttributeModifier modifier : modifiers.get(Attribute.ARMOR_TOUGHNESS)) {
                toughness += modifier.getAmount();
            }

            // 근접 물리 데미지이므로 보호(PROTECTION)만 유효 (EPF = 레벨 합, 최대 20)
            epf += item.getEnchantmentLevel(Enchantment.PROTECTION);
        }

        // ① 방어 포인트 감소 (바닐라 1.9+ 공식)
        double reduction = Math.min(20.0,
                Math.max(defense / 5.0, defense - baseDamage / (2.0 + toughness / 4.0)));
        double afterArmor = baseDamage * (1.0 - reduction / 25.0);

        // ② 보호 인챈트 감소 (EPF 1당 4%, 최대 80%)
        return afterArmor * (1.0 - Math.min(20, epf) / 25.0);
    }

    /** 방어구를 하나라도 착용 중인지 (액션바 🛡 표시용) */
    public static boolean hasArmor(ArmorStand dummy) {
        EntityEquipment equipment = dummy.getEquipment();
        for (EquipmentSlot slot : DummyManager.ARMOR_SLOTS) {
            if (!equipment.getItem(slot).getType().isAir()) {
                return true;
            }
        }
        return false;
    }
}
