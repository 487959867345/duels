package io.github.best.duel.game;

import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;

public class Kits {
    public enum Basic implements ItemConvertible {
        HEAD(Items.DIAMOND_BOOTS),
        MAIN(Items.DIAMOND_SWORD),
        CHEST(Items.DIAMOND_CHESTPLATE),
        LEGS(Items.DIAMOND_LEGGINGS),
        BOOTS(Items.DIAMOND_BOOTS);

        Basic(Item item) {

        }

        @Override
        public Item asItem() {
            return null;
        }
    }
}
