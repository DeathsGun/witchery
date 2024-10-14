package dev.sterner.witchery.recipe.ritual

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.sterner.witchery.block.ritual.RitualManager
import dev.sterner.witchery.block.ritual.RitualManager.CommandType
import dev.sterner.witchery.recipe.MultipleItemRecipeInput
import dev.sterner.witchery.recipe.cauldron.CauldronBrewingRecipe
import dev.sterner.witchery.recipe.cauldron.ItemStackWithColor
import dev.sterner.witchery.registry.WitcheryRecipeSerializers
import dev.sterner.witchery.registry.WitcheryRecipeTypes
import net.minecraft.core.HolderLookup
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.*
import net.minecraft.world.level.Level

class RitualRecipe(val inputItems: List<ItemStack>, val outputItems: List<ItemStack>, val altarPower: Int, val commands: Set<CommandType>) :
    Recipe<MultipleItemRecipeInput> {

    override fun matches(input: MultipleItemRecipeInput, level: Level): Boolean {
        return true
    }

    override fun assemble(input: MultipleItemRecipeInput, registries: HolderLookup.Provider): ItemStack {
        return ItemStack.EMPTY
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean {
        return true
    }

    override fun getResultItem(registries: HolderLookup.Provider): ItemStack {
       return ItemStack.EMPTY
    }

    override fun getSerializer(): RecipeSerializer<*> {
        return WitcheryRecipeSerializers.RITUAL_RECIPE_SERIALIZER.get()
    }

    override fun getType(): RecipeType<*> {
        return WitcheryRecipeTypes.RITUAL_RECIPE_TYPE.get()
    }

    class Serializer : RecipeSerializer<RitualRecipe> {
        override fun codec(): MapCodec<RitualRecipe> {
            return CODEC
        }

        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, RitualRecipe> {
            return STREAM_CODEC
        }

        companion object {

            val COMMAND_TYPE_CODEC: Codec<CommandType> = RecordCodecBuilder.create { instance ->
                instance.group(
                    Codec.STRING.fieldOf("command").forGetter(CommandType::command),
                    Codec.STRING.fieldOf("type").forGetter(CommandType::type)
                ).apply(instance, RitualManager::CommandType)
            }

            val COMMANDS_SET_CODEC: Codec<Set<CommandType>> = COMMAND_TYPE_CODEC.listOf().xmap(
                { it.toSet() },
                { it.toList() }
            )

            val CODEC: MapCodec<RitualRecipe> =
                RecordCodecBuilder.mapCodec { obj: RecordCodecBuilder.Instance<RitualRecipe> ->
                    obj.group(
                        ItemStack.STRICT_SINGLE_ITEM_CODEC.listOf().fieldOf("inputItems").forGetter { it.inputItems },
                        ItemStack.STRICT_SINGLE_ITEM_CODEC.listOf().fieldOf("outputItems").forGetter { it.outputItems },
                        Codec.INT.fieldOf("altarPower").forGetter { recipe -> recipe.altarPower },
                        COMMANDS_SET_CODEC.fieldOf("commands").forGetter { recipe -> recipe.commands }
                    ).apply(obj, ::RitualRecipe)
                }

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, RitualRecipe> =
                ByteBufCodecs.fromCodecWithRegistries(
                    CODEC.codec()
                )
        }
    }

    companion object {
        const val NAME: String = "cauldron_brewing"
    }

}