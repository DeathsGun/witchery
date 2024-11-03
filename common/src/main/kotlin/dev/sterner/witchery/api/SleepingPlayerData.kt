package dev.sterner.witchery.api

import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.*


class SleepingPlayerData(
    var id: UUID? = UUID(0,0),
    var playerUuid: UUID? = UUID(0,0),
    var playerName: String? = "",
    var mainInventory: NonNullList<ItemStack> = NonNullList.withSize(36, ItemStack.EMPTY),
    var armorInventory: NonNullList<ItemStack> = NonNullList.withSize(4, ItemStack.EMPTY),
    var offHandInventory: NonNullList<ItemStack> = NonNullList.withSize(1,ItemStack.EMPTY),
    var equipment: NonNullList<ItemStack> = NonNullList.withSize(EquipmentSlot.entries.size, ItemStack.EMPTY),
    var extraInventory: NonNullList<ItemStack> = NonNullList.withSize(36 + 9,ItemStack.EMPTY),
    var model: Byte = 0,
){

    fun writeNbt(lookup: HolderLookup.Provider): CompoundTag {
        val nbt = CompoundTag()
        this.id?.let { nbt.putUUID("Id", it) }
        this.playerUuid?.let { nbt.putUUID("Uuid", it) }
        this.playerName?.let { nbt.putString("Name", it) }
        nbt.putByte("Model", this.model)

        writeInventory(nbt, "Main", this.mainInventory, lookup)
        writeInventory(nbt, "Armor", this.armorInventory, lookup)
        writeInventory(nbt, "Offhand", this.offHandInventory, lookup)
        writeInventory(nbt, "Extra", this.extraInventory, lookup)
        writeInventory(nbt, "Equipment", this.equipment, lookup)

        return nbt
    }

    companion object {

        fun fromPlayer(player: Player) : SleepingPlayerData {
            val builder = SleepingPlayerData()
            builder.id = UUID.randomUUID()
            builder.playerUuid = player.uuid
            builder.playerName = player.name.string

            for (i in 0 until builder.mainInventory.size) {
                builder.mainInventory[i] = player.inventory.items[i]
            }

            for (i in 0 until builder.armorInventory.size) {
                builder.armorInventory[i] = player.inventory.armor[i]
            }

            for (i in 0 until builder.offHandInventory.size) {
                builder.offHandInventory[i] = player.inventory.offhand[i]
            }

            for (i in 0 until EquipmentSlot.entries.size) {
                builder.equipment[i] = player.getItemBySlot(EquipmentSlot.entries[i]).copy()
            }

            builder.model = player.entityData.get(Player.DATA_PLAYER_MODE_CUSTOMISATION)
            return builder
        }

        fun readNbt(nbt: CompoundTag, lookup: HolderLookup.Provider): SleepingPlayerData {
            val builder = SleepingPlayerData()
            if (nbt.contains("Id")) {
                builder.id = nbt.getUUID("Id")
            } else {
                builder.id = UUID.randomUUID()
            }

            if (nbt.contains("Uuid")) {
                builder.playerUuid = nbt.getUUID("Uuid")
            } else {
                builder.playerUuid = UUID.randomUUID()
            }

            builder.playerName = nbt.getString("Name")

            readInventory(nbt, "Main", builder.mainInventory, lookup)
            readInventory(nbt, "Armor", builder.armorInventory, lookup)
            readInventory(nbt, "Offhand", builder.offHandInventory, lookup)
            readInventory(nbt, "Equipment", builder.equipment, lookup)
            readInventory(nbt, "Extra", builder.extraInventory, lookup)

            builder.model = nbt.getByte("Model")
            return builder
        }

        private fun readInventory(compound: CompoundTag, name: String, inv: NonNullList<ItemStack>, lookup: HolderLookup.Provider) {

            if (compound.contains(name, 9)) {
                val listTag = compound.getList(name, 10)

                for (i in inv.indices) {
                    val compoundTag = listTag.getCompound(i)
                    inv[i] = ItemStack.parseOptional(lookup, compoundTag)
                }
            }
        }

        private fun writeInventory(nbt: CompoundTag, key: String, inventory: NonNullList<ItemStack>, lookup: HolderLookup.Provider) {
            val listTag = ListTag()

            for (itemStack in inventory) {
                if (!itemStack.isEmpty) {
                    listTag.add(itemStack.save(lookup))
                } else {
                    listTag.add(CompoundTag())
                }
            }

            nbt.put(key, listTag)
        }
    }
}