package dev.sterner.witchery.registry

import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import dev.sterner.witchery.Witchery
import dev.sterner.witchery.api.multiblock.MultiBlockComponentBlock
import dev.sterner.witchery.block.*
import dev.sterner.witchery.block.altar.AltarBlock
import dev.sterner.witchery.block.altar.AltarBlockComponent
import dev.sterner.witchery.block.cauldron.CauldronBlock
import dev.sterner.witchery.block.cauldron.CauldronBlockComponent
import dev.sterner.witchery.block.oven.OvenBlock
import dev.sterner.witchery.block.oven.OvenFumeExtensionBlock
import dev.sterner.witchery.block.oven.OvenFumeExtensionBlockComponent
import dev.sterner.witchery.block.ritual.GoldenChalkBlock
import dev.sterner.witchery.block.ritual.RitualChalkBlock
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.material.PushReaction
import java.awt.Color


object WitcheryBlocks {

    val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(Witchery.MODID, Registries.BLOCK)

    val COMPONENT: RegistrySupplier<MultiBlockComponentBlock> = BLOCKS.register("component") {
        MultiBlockComponentBlock(BlockBehaviour.Properties.of())
    }

    val DEEPLSTAE_ALTAR_BLOCK = BLOCKS.register("deepslate_altar_block") {
        AltarCreationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE))
    }

    val ALTAR: RegistrySupplier<AltarBlock> = BLOCKS.register("altar") {
        AltarBlock(BlockBehaviour.Properties.of())
    }

    val ALTAR_COMPONENT: RegistrySupplier<AltarBlockComponent> = BLOCKS.register("altar_component") {
        AltarBlockComponent(BlockBehaviour.Properties.of())
    }

    val CAULDRON: RegistrySupplier<CauldronBlock> = BLOCKS.register("cauldron") {
        CauldronBlock(BlockBehaviour.Properties.of()
            .sound(SoundType.METAL)
        )
    }

    val CAULDRON_COMPONENT: RegistrySupplier<CauldronBlockComponent> = BLOCKS.register("cauldron_component") {
        CauldronBlockComponent(BlockBehaviour.Properties.of()
            .sound(SoundType.METAL)
        )
    }

    val IRON_WITCHES_OVEN: RegistrySupplier<OvenBlock> = BLOCKS.register("iron_witches_oven") {
        OvenBlock(BlockBehaviour.Properties.of()
            .sound(SoundType.METAL)
        )
    }

    val IRON_WITCHES_OVEN_FUME_EXTENSION: RegistrySupplier<OvenFumeExtensionBlock> = BLOCKS.register("iron_witches_oven_fume_extension") {
        OvenFumeExtensionBlock(BlockBehaviour.Properties.of()
            .sound(SoundType.METAL)
        )
    }

    val IRON_WITCHES_OVEN_FUME_EXTENSION_COMPONENT: RegistrySupplier<OvenFumeExtensionBlockComponent> = BLOCKS.register("iron_witches_oven_fume_extension_component") {
        OvenFumeExtensionBlockComponent(BlockBehaviour.Properties.of()
            .sound(SoundType.METAL)
        )
    }

    val COPPER_WITCHES_OVEN: RegistrySupplier<OvenBlock> = BLOCKS.register("copper_witches_oven") {
        OvenBlock(BlockBehaviour.Properties.of()
            .sound(SoundType.METAL)
        )
    }

    val GLINTWEED: RegistrySupplier<GlintweedBlock> = BLOCKS.register("glintweed") {
        GlintweedBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY))
    }

    val EMBER_MOSS: RegistrySupplier<EmbermossBlock> = BLOCKS.register("ember_moss") {
        EmbermossBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY))
    }

    val SPANISH_MOSS: RegistrySupplier<SpanishMossBlock> = BLOCKS.register("spanish_moss") {
        SpanishMossBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
            .replaceable()
            .noCollission()
            .randomTicks()
            .strength(0.2F)
            .sound(SoundType.VINE)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY))
    }

    val MANDRAKE_CROP = BLOCKS.register("mandrake") {
        MandrakeCropBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY)
        )
    }

    val BELLADONNAE_CROP = BLOCKS.register("belladonna") {
        WitcheryCropBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY)
        )
    }

    val SNOWBELL_CROP = BLOCKS.register("snowbell") {
        WitcheryCropBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY)
        )
    }

    val WATER_ARTICHOKE_CROP = BLOCKS.register("water_artichoke") {
        WaterArtichokeCropBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY)
        )
    }

    val WOLFSFBANE_CROP = BLOCKS.register("wolfsbane") {
        WitcheryCropBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY)
        )
    }

    val GARLIC_CROP = BLOCKS.register("garlic") {
        WitcheryCropBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY)
        )
    }

    val WORMWOOD_CROP = BLOCKS.register("wormwood") {
        WormwoodCropBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY)
        )
    }

    val RITUAL_CHALK_BLOCK = BLOCKS.register("ritual_chalk") {
        RitualChalkBlock(null, 0xFFFFFF, BlockBehaviour.Properties.of())
    }

    val INFERNAL_CHALK_BLOCK = BLOCKS.register("infernal_chalk") {
        RitualChalkBlock(ParticleTypes.FLAME, Color(230,0,75).rgb, BlockBehaviour.Properties.of())
    }

    val OTHERWHERE_CHALK_BLOCK = BLOCKS.register("otherwhere_chalk") {
        RitualChalkBlock(ParticleTypes.PORTAL, Color(190,55,250).rgb, BlockBehaviour.Properties.of())
    }

    val GOLDEN_CHALK_BLOCK = BLOCKS.register("golden_chalk") {
        GoldenChalkBlock(BlockBehaviour.Properties.of())
    }
}