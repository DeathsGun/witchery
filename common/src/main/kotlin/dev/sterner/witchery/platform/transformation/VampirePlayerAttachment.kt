package dev.sterner.witchery.platform.transformation

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.architectury.injectables.annotations.ExpectPlatform
import dev.architectury.networking.NetworkManager
import dev.sterner.witchery.Witchery
import dev.sterner.witchery.payload.DismountBroomC2SPayload
import dev.sterner.witchery.payload.SyncVampireS2CPacket
import dev.sterner.witchery.payload.VampireAbilitySelectionC2SPayload
import dev.sterner.witchery.registry.WitcheryPayloads
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.StringRepresentable
import net.minecraft.world.entity.player.Player

object VampirePlayerAttachment {

    @ExpectPlatform
    @JvmStatic
    fun getData(player: Player): Data {
        throw AssertionError()
    }

    @ExpectPlatform
    @JvmStatic
    fun setData(player: Player, data: Data) {
        throw AssertionError()
    }

    @JvmStatic
    fun setMaxBlood(player: Player){
        val data = getData(player)

        val toSet = when (data.vampireLevel) {
            1 -> 900
            2 -> 1200
            3 -> 1500
            4 -> 1500
            5 -> 1800
            6 -> 2100
            7 -> 2400
            8 -> 2700
            9 -> 3000
            10 -> 3600
            else -> 0
        }

        val bloodData = BloodPoolLivingEntityAttachment.getData(player)

        BloodPoolLivingEntityAttachment.setData(player, BloodPoolLivingEntityAttachment.Data(toSet, bloodData.bloodPool))
    }

    fun setAbilityIndex(player: Player, abilityIndex: Int) {
        val data = getData(player)
        setData(player, Data(data.vampireLevel, data.killedBlazes, data.usedSunGrenades, data.villagersHalfBlood, data.nightsCount, data.visitedVillages, data.trappedVillagers, abilityIndex))
        NetworkManager.sendToServer(VampireAbilitySelectionC2SPayload(abilityIndex))
    }

    fun getAbilities(player: Player): List<VampireAbility> {
        val level = getData(player).vampireLevel
        return VampireAbility.entries.filter { it.unlockLevel <= level }
    }

    fun sync(player: Player, data: Data) {
        if (player.level() is ServerLevel) {
            WitcheryPayloads.sendToPlayers(player.level(), player.blockPosition(), SyncVampireS2CPacket(player, data))
        }
    }

    fun tick(player: Player?) {
        if (player != null && player.level() is ServerLevel) {

        }
    }

    @JvmStatic
    fun increaseVampireLevel(player: Player) {
        val data = getData(player)
        setData(player, data.copy(vampireLevel = data.vampireLevel + 1))
        setMaxBlood(player)
    }

    @JvmStatic
    fun increaseKilledBlazes(player: Player) {
        val data = getData(player)
        setData(player, data.copy(killedBlazes = data.killedBlazes + 1))
    }

    @JvmStatic
    fun increaseUsedSunGrenades(player: Player) {
        val data = getData(player)
        setData(player, data.copy(usedSunGrenades = data.usedSunGrenades + 1))
    }

    @JvmStatic
    fun increaseVillagersHalfBlood(player: Player) {
        val data = getData(player)
        setData(player, data.copy(villagersHalfBlood = data.villagersHalfBlood + 1))
    }

    @JvmStatic
    fun increaseNightsCount(player: Player) {
        val data = getData(player)
        setData(player, data.copy(nightsCount = data.nightsCount + 1))
    }

    @JvmStatic
    fun increaseVisitedVillages(player: Player) {
        val data = getData(player)
        setData(player, data.copy(visitedVillages = data.visitedVillages + 1))
    }

    @JvmStatic
    fun increaseTrappedVillagers(player: Player) {
        val data = getData(player)
        setData(player, data.copy(trappedVillagers = data.trappedVillagers + 1))
    }

    data class Data(
        val vampireLevel: Int = 7, //TODO set to 0
        val killedBlazes: Int = 0,
        val usedSunGrenades: Int = 0,
        val villagersHalfBlood: Int = 0,
        val nightsCount: Int = 0,
        val visitedVillages: Int = 0,
        val trappedVillagers: Int = 0,
        val abilityIndex: Int = -1
    ) {

        companion object {
            val CODEC: Codec<Data> = RecordCodecBuilder.create { instance ->
                instance.group(
                    Codec.INT.fieldOf("vampireLevel").forGetter { it.vampireLevel },
                    Codec.INT.fieldOf("killedBlazes").forGetter { it.killedBlazes },
                    Codec.INT.fieldOf("usedSunGrenades").forGetter { it.usedSunGrenades },
                    Codec.INT.fieldOf("villagersHalfBlood").forGetter { it.villagersHalfBlood },
                    Codec.INT.fieldOf("nightsCount").forGetter { it.nightsCount },
                    Codec.INT.fieldOf("visitedVillages").forGetter { it.visitedVillages },
                    Codec.INT.fieldOf("trappedVillagers").forGetter { it.trappedVillagers },
                    Codec.INT.fieldOf("abilityIndex").forGetter { it.abilityIndex },
                ).apply(instance, ::Data)
            }

            val ID: ResourceLocation = Witchery.id("vampire_player_data")
        }
    }

    enum class VampireAbility(val unlockLevel: Int) : StringRepresentable {
        DRINK_BLOOD(1),
        TRANSFIX(2),
        SPEED(4),
        BAT_FORM(7);

        override fun getSerializedName(): String {
            return name.lowercase()
        }
    }
}