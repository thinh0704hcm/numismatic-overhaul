package com.glisco.numismaticoverhaul.mixin;


import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;
import org.spongepowered.asm.mixin.Mixin;





@Mixin(ItemStackComponentizationFix.class)
public class ItemStackComponentizationFixin {


    // TODO: Migration to data components removed due to private ItemStackData access in MC 26.2
}
