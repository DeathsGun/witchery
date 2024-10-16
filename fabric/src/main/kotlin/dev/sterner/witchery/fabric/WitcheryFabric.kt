package dev.sterner.witchery.fabric

import dev.sterner.witchery.Witchery
import dev.sterner.witchery.api.attachment.AltarAttachmentData
import dev.sterner.witchery.api.attachment.MutandisAttachmentData
import dev.sterner.witchery.client.particle.ColorBubbleParticle
import dev.sterner.witchery.platform.MutandisLevelDataAttachmentPlatform
import dev.sterner.witchery.registry.WitcheryBlocks
import dev.sterner.witchery.registry.WitcheryFlammability
import dev.sterner.witchery.registry.WitcheryParticleTypes
import dev.sterner.witchery.registry.WitcheryRitualRegistry
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteProvider
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry
import net.fabricmc.fabric.api.event.registry.DynamicRegistries
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry


class WitcheryFabric : ModInitializer, ClientModInitializer {

    companion object {
        @Suppress("UnstableApiUsage")
        val MUTANDIS_LEVEL_DATA_TYPE: AttachmentType<MutandisAttachmentData> =
            AttachmentRegistry.builder<MutandisAttachmentData>()
                .persistent(MutandisLevelDataAttachmentPlatform.CODEC)
                .initializer { MutandisAttachmentData() }
                .buildAndRegister(MutandisLevelDataAttachmentPlatform.ID)

        @Suppress("UnstableApiUsage")
        val ALTAR_LEVEL_DATA_TYPE: AttachmentType<AltarAttachmentData> =
            AttachmentRegistry.builder<AltarAttachmentData>()
                .persistent(AltarAttachmentData.CODEC)
                .initializer { AltarAttachmentData() }
                .buildAndRegister(AltarAttachmentData.ID)
    }

    override fun onInitialize() {
        Witchery.init()

        DynamicRegistries.registerSynced(WitcheryRitualRegistry.RITUAL_KEY, WitcheryRitualRegistry.CODEC)

        StrippableBlockRegistry.register(WitcheryBlocks.ROWAN_LOG.get(), WitcheryBlocks.STRIPPED_ROWAN_LOG.get())
        StrippableBlockRegistry.register(WitcheryBlocks.ROWAN_WOOD.get(), WitcheryBlocks.STRIPPED_ROWAN_WOOD.get())

        WitcheryFlammability.register()
    }

    override fun onInitializeClient() {
        Witchery.initClient()

        ParticleFactoryRegistry.getInstance().register(
            WitcheryParticleTypes.COLOR_BUBBLE.get()
        ) { sprite: FabricSpriteProvider? ->
            ColorBubbleParticle.Provider(
                sprite!!
            )
        }
    }
}


