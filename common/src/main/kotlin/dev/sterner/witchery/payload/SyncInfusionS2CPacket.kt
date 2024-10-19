package dev.sterner.witchery.payload

import dev.architectury.networking.NetworkManager
import dev.sterner.witchery.Witchery
import dev.sterner.witchery.platform.infusion.InfusionData
import dev.sterner.witchery.platform.infusion.InfusionType
import dev.sterner.witchery.platform.infusion.PlayerInfusionDataAttachment
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

class SyncInfusionS2CPacket(val nbt: CompoundTag) : CustomPacketPayload {

    constructor(friendlyByteBuf: RegistryFriendlyByteBuf) : this(friendlyByteBuf.readNbt()!!)

    constructor(data: InfusionData): this(CompoundTag().apply {
        putInt("Charge", data.charge)
        putString("Type", data.type.serializedName) // serializedName should be in lowercase
    })

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return ID
    }

    private fun write(friendlyByteBuf: RegistryFriendlyByteBuf?) {
        friendlyByteBuf?.writeNbt(nbt)
    }

    fun handleS2C(payload: SyncInfusionS2CPacket, context: NetworkManager.PacketContext) {
        val client = Minecraft.getInstance()

        val charge = payload.nbt.getInt("Charge")
        val type = InfusionType.valueOf(payload.nbt.getString("Type").uppercase())

        client.execute {
            PlayerInfusionDataAttachment.setPlayerInfusion(context.player, InfusionData(type, charge))
        }
    }

    companion object {
        val ID: CustomPacketPayload.Type<SyncInfusionS2CPacket> =
            CustomPacketPayload.Type(Witchery.id("sync_infusion"))

        val STREAM_CODEC: StreamCodec<in RegistryFriendlyByteBuf?, SyncInfusionS2CPacket> =
            CustomPacketPayload.codec(
                { payload, buf -> payload.write(buf) },
                { buf -> SyncInfusionS2CPacket(buf!!) }
            )
    }
}