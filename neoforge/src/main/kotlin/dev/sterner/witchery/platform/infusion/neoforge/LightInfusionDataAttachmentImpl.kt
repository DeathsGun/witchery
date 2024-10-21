package dev.sterner.witchery.platform.infusion.neoforge

import dev.sterner.witchery.neoforge.WitcheryNeoForge.LIGHT_INFUSION_PLAYER_DATA_ATTACHMENT
import dev.sterner.witchery.platform.infusion.LightInfusionData
import dev.sterner.witchery.platform.infusion.LightInfusionDataAttachment
import net.minecraft.world.entity.player.Player

object LightInfusionDataAttachmentImpl {

    @JvmStatic
    fun setInvisible(player: Player, invisible: Boolean, invisibleTicks: Int){
        val data = LightInfusionData(invisible, invisibleTicks)
        player.setData(LIGHT_INFUSION_PLAYER_DATA_ATTACHMENT, data)
        LightInfusionDataAttachment.sync(player, data)
    }

    @JvmStatic
    fun isInvisible(player: Player): LightInfusionData {
        return player.getData(LIGHT_INFUSION_PLAYER_DATA_ATTACHMENT)
    }

}