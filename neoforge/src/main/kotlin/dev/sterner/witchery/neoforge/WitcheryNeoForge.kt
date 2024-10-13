package dev.sterner.witchery.neoforge

import dev.sterner.witchery.Witchery
import dev.sterner.witchery.client.model.AltarBlockEntityModel
import dev.sterner.witchery.client.model.AltarClothBlockEntityModel
import dev.sterner.witchery.client.particle.ColorBubbleParticle
import dev.sterner.witchery.client.screen.OvenScreen
import dev.sterner.witchery.registry.WitcheryBlocks
import dev.sterner.witchery.registry.WitcheryMenuTypes
import dev.sterner.witchery.registry.WitcheryParticleTypes
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.RenderType
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent
import net.neoforged.neoforge.registries.DataPackRegistryEvent.NewRegistry
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist


@Mod(Witchery.MODID)
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
object WitcheryNeoForge {

    init {
        Witchery.init()

        runForDist(
            clientTarget = {
                MOD_BUS.addListener(::onClientSetup)
                Minecraft.getInstance()
            },
            serverTarget = {
                MOD_BUS.addListener(::onServerSetup)
                "test"
            }
        )
    }

    private fun onServerSetup(event: FMLDedicatedServerSetupEvent) {

    }

    @SubscribeEvent
    fun registerParticle(event: RegisterParticleProvidersEvent){
        event.registerSpriteSet(WitcheryParticleTypes.COLOR_BUBBLE.get()){ o ->
            ColorBubbleParticle.Provider(o)
        }
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        Witchery.initClient()
    }

    @SubscribeEvent
    private fun initializeClient(event: RegisterClientExtensionsEvent) {
        ItemBlockRenderTypes.setRenderLayer(WitcheryBlocks.CAULDRON.get(), RenderType.cutout())
        ItemBlockRenderTypes.setRenderLayer(WitcheryBlocks.GLINTWEED.get(), RenderType.cutout())
        ItemBlockRenderTypes.setRenderLayer(WitcheryBlocks.EMBER_MOSS.get(), RenderType.cutout())
        ItemBlockRenderTypes.setRenderLayer(WitcheryBlocks.SPANISH_MOSS.get(), RenderType.cutout())
        ItemBlockRenderTypes.setRenderLayer(WitcheryBlocks.MANDRAKE_CROP.get(), RenderType.cutout())
        ItemBlockRenderTypes.setRenderLayer(WitcheryBlocks.BELLADONNAE_CROP.get(), RenderType.cutout())
        ItemBlockRenderTypes.setRenderLayer(WitcheryBlocks.SNOWBELL_CROP.get(), RenderType.cutout())
        ItemBlockRenderTypes.setRenderLayer(WitcheryBlocks.COPPER_WITCHES_OVEN.get(), RenderType.cutout())
        ItemBlockRenderTypes.setRenderLayer(WitcheryBlocks.IRON_WITCHES_OVEN.get(), RenderType.cutout())
        ItemBlockRenderTypes.setRenderLayer(WitcheryBlocks.WATER_ARTICHOKE_CROP.get(), RenderType.cutout())
        ItemBlockRenderTypes.setRenderLayer(WitcheryBlocks.WOLFSFBANE_CROP.get(), RenderType.cutout())
        ItemBlockRenderTypes.setRenderLayer(WitcheryBlocks.GARLIC_CROP.get(), RenderType.cutout())
        ItemBlockRenderTypes.setRenderLayer(WitcheryBlocks.WORMWOOD_CROP.get(), RenderType.cutout())
        ItemBlockRenderTypes.setRenderLayer(WitcheryBlocks.IRON_WITCHES_OVEN_FUME_EXTENSION.get(), RenderType.cutout())
    }

    @SubscribeEvent
    private fun registerLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(
            AltarBlockEntityModel.LAYER_LOCATION,
            AltarBlockEntityModel::createBodyLayer)
        event.registerLayerDefinition(
            AltarClothBlockEntityModel.LAYER_LOCATION,
            AltarClothBlockEntityModel::createBodyLayer)
    }

    @SubscribeEvent
    fun createDataPackRegistries(event: NewRegistry) {
    }

    @SubscribeEvent
    private fun registerScreens(event: RegisterMenuScreensEvent) {
        event.register(WitcheryMenuTypes.OVEN_MENU_TYPE.get()) { arg, arg2, arg3 ->
            OvenScreen(arg, arg2, arg3)
        }
    }
}
