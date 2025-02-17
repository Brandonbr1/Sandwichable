package io.github.foundationgames.sandwichable.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(StructurePool.class)
public interface StructurePoolAccess {
    @Accessor(value = "elements")
    List<StructurePoolElement> sandwichable$getElements();

    @Accessor(value = "elementCounts")
    List<Pair<StructurePoolElement, Integer>> sandwichable$getElementCounts();
}
