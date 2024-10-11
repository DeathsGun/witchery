package dev.sterner.witchery.block.cauldron

import dev.architectury.fluid.FluidStack
import dev.architectury.hooks.fluid.FluidStackHooks
import dev.architectury.networking.NetworkManager
import dev.sterner.witchery.api.block.WitcheryFluidTank
import dev.sterner.witchery.api.multiblock.MultiBlockCoreEntity
import dev.sterner.witchery.payload.CauldronPoofS2CPacket
import dev.sterner.witchery.payload.SyncCauldronS2CPacket
import dev.sterner.witchery.recipe.CauldronBrewingRecipe
import dev.sterner.witchery.recipe.CauldronCraftingRecipe
import dev.sterner.witchery.recipe.IngredientWithColor
import dev.sterner.witchery.recipe.MultipleItemRecipeInput
import dev.sterner.witchery.registry.WitcheryBlockEntityTypes
import dev.sterner.witchery.registry.WitcheryPayloads
import dev.sterner.witchery.registry.WitcheryRecipeTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.*
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.AABB


class CauldronBlockEntity(pos: BlockPos, state: BlockState) : MultiBlockCoreEntity(
    WitcheryBlockEntityTypes.CAULDRON.get(), CauldronBlock.STRUCTURE.get(),
    pos, state
), Container {

    private var cauldronCraftingRecipe: CauldronCraftingRecipe? = null
    private var cauldronBrewingRecipe: CauldronBrewingRecipe? = null
    private var inputItems: NonNullList<ItemStack> = NonNullList.withSize(12, ItemStack.EMPTY)
    private var craftingProgressTicker = 0
    private var brewItemOutput: ItemStack = ItemStack.EMPTY

    var color = WATER_COLOR
    var fluidTank = WitcheryFluidTank(this)
    private var complete = false

    override fun init(level: Level, pos: BlockPos, state: BlockState) {
        refreshCraftingAndBrewingRecipe(level)
    }

    private fun refreshCraftingAndBrewingRecipe(level: Level) {
        val allRecipesOfType = level.recipeManager.getAllRecipesFor(WitcheryRecipeTypes.CAULDRON_RECIPE_TYPE.get())
        val nonEmptyItems = inputItems.filter { !it.isEmpty }

        // Find the possible recipe based on current input items
        val possibleRecipe = allRecipesOfType.firstOrNull { it.value.matches(MultipleItemRecipeInput(nonEmptyItems), level) }

        // If a recipe is found and the order is correct, set cauldronCraftingRecipe
        possibleRecipe?.let { recipe ->
            val isOrderCorrect = isOrderRight(nonEmptyItems, recipe.value.inputItems)

            if (isOrderCorrect) {
                cauldronCraftingRecipe = recipe.value // Set the recipe even if incomplete
                complete = nonEmptyItems.size == recipe.value.inputItems.size // Only complete if all items are matched
            } else {
                refreshBrewingRecipe(level)
            }
        } ?: run {
            // If no crafting recipe matches, try the brewing recipe
            refreshBrewingRecipe(level)
        }

        setChanged()
    }

    private fun refreshBrewingRecipe(level: Level) {
        val allRecipesOfType = level.recipeManager.getAllRecipesFor(WitcheryRecipeTypes.CAULDRON_BREWING_RECIPE_TYPE.get())
        val nonEmptyItems = inputItems.filter { !it.isEmpty }

        // Find the possible recipe based on current input items
        val possibleRecipe = allRecipesOfType.firstOrNull { it.value.matches(MultipleItemRecipeInput(nonEmptyItems), level) }

        // If a recipe is found and the order is correct, set cauldronBrewingRecipe
        possibleRecipe?.let { recipe ->
            val isOrderCorrect = isOrderRight(nonEmptyItems, recipe.value.inputItems)

            if (isOrderCorrect) {
                cauldronBrewingRecipe = recipe.value // Set the recipe even if incomplete
                complete = nonEmptyItems.size == recipe.value.inputItems.size // Only complete if all items are matched
            } else {
                cauldronBrewingRecipe = null // Reset if the order is wrong
                complete = false
            }
        } ?: run {
            // If no brewing recipe matches, reset to null and incomplete
            cauldronBrewingRecipe = null
            complete = false
        }

        setChanged()
    }

    override fun tick(level: Level, pos: BlockPos, state: BlockState) {
        super.tick(level, pos, state)

        if (level.isClientSide || !state.getValue(BlockStateProperties.LIT)) {
            return
        }

        if (level.gameTime % 4 == 0L && !complete && !fluidTank.fluidStorage.isEmpty()) {
            consumeItem(level, pos)
        }

        if (complete && !brewItemOutput.isEmpty) {
            //TODO add spiraling enchantment particle effect
        }

        // Handle crafting progress and execution
        if (cauldronCraftingRecipe != null || cauldronBrewingRecipe != null) {
            // Only start ticking when the recipe is complete
            if (complete) {
                if (craftingProgressTicker < PROGRESS_TICKS) {
                    craftingProgressTicker++
                    setChanged()
                } else {
                    craftingProgressTicker = 0
                    craft(level, pos)
                }
            }
        }
    }

    /**
     * Checks within the cauldron if there's any item entities, adds them to the inventory,
     * check for recipes and applies appropriate color
     */
    private fun consumeItem(level: Level, pos: BlockPos) {
        level.getEntities(EntityType.ITEM, AABB(blockPos)) { true }.forEach { entity ->
            val item = entity.item
            val cacheForColorItem = item.copy()

            // Get the first empty slot and add the item to inputItems
            val firstEmpty = getFirstEmptyIndex()
            if (firstEmpty != -1) {
                setItem(firstEmpty, item.split(1))
                level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.35f, 1f)

                // Refresh recipe to match current inputItems
                refreshCraftingAndBrewingRecipe(level)

                updateColor(level, cacheForColorItem)
            }

            setChanged()
        }
    }

    private fun updateColor(level: Level, cacheForColorItem: ItemStack) {
        // Default color to brown (indicating no correct order match)
        var colorSet = false

        // Get all recipes for crafting and brewing
        val allCraftingRecipes = level.recipeManager.getAllRecipesFor(WitcheryRecipeTypes.CAULDRON_RECIPE_TYPE.get())
        val allBrewingRecipes = level.recipeManager.getAllRecipesFor(WitcheryRecipeTypes.CAULDRON_BREWING_RECIPE_TYPE.get())
        val nonEmptyItems = inputItems.filter { !it.isEmpty }

        // Check crafting recipes
        allCraftingRecipes.forEach { recipe ->
            recipe.value.inputItems.forEach { ingredientWithColor ->
                // Check if the ingredient matches and the order is correct
                val orderIsCorrect = isOrderRight(nonEmptyItems, recipe.value.inputItems)
                if (ingredientWithColor.ingredient.test(cacheForColorItem) && orderIsCorrect) {
                    color = ingredientWithColor.color // Set color based on the matched ingredient
                    colorSet = true // Flag that a color was successfully set
                }
            }
        }

        // Check brewing recipes if no crafting match was found
        if (!colorSet) {
            allBrewingRecipes.forEach { recipe ->
                recipe.value.inputItems.forEach { ingredientWithColor ->
                    // Check if the ingredient matches and the order is correct
                    val orderIsCorrect = isOrderRight(nonEmptyItems, recipe.value.inputItems)
                    if (ingredientWithColor.ingredient.test(cacheForColorItem) && orderIsCorrect) {
                        color = ingredientWithColor.color // Set color based on the matched ingredient
                        colorSet = true // Flag that a color was successfully set
                    }
                }
            }
        }

        // If no recipe fully or partially matches, set the color to brown
        if (!colorSet) {
            color = 0x5a2d0d // Set color to brown if no matching order is found
        }
    }

    private fun craft(level: Level, pos: BlockPos) {
        val itemsToCraft = cauldronCraftingRecipe?.outputItems

        for (item in itemsToCraft ?: emptyList()) {
            val list = NonNullList.create<ItemStack>()

            item.items.forEach { stack ->
                list.add(stack.copy())
            }

            for (drop in list) {
                Containers.dropItemStack(level, pos.x + 0.5, pos.y + 1.1, pos.z + 0.5, drop)
            }
        }

        WitcheryPayloads.sendToPlayers(level, pos, SyncCauldronS2CPacket(pos))

        if (cauldronCraftingRecipe != null) {
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5f, 1.0f)
            spawnSmokeParticle(level, pos)
        }

        resetCauldronPartial()
    }

    fun resetCauldronPartial() {
        if (cauldronCraftingRecipe != null) {
            fluidTank = WitcheryFluidTank(this)
            brewItemOutput = ItemStack.EMPTY
        }

        if (cauldronBrewingRecipe != null) {
            brewItemOutput = cauldronBrewingRecipe!!.outputItem
        } else {
            color = WATER_COLOR
        }

        clearContent()
        cauldronCraftingRecipe = null
        cauldronBrewingRecipe = null
        complete = false
        setChanged()
    }

    private fun fullReset(){
        color = WATER_COLOR
        clearContent()
        cauldronCraftingRecipe = null
        cauldronBrewingRecipe = null
        complete = false
        fluidTank = WitcheryFluidTank(this)
        brewItemOutput = ItemStack.EMPTY
        setChanged()
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return fluidTank.getUpdateTag(super.getUpdateTag(registries), registries)
    }

    override fun onUseWithItem(pPlayer: Player, pStack: ItemStack, pHand: InteractionHand): ItemInteractionResult {
        if (pStack.`is`(Items.FLINT_AND_STEEL)) {
            playSound(level, pPlayer, blockPos, SoundEvents.FLINTANDSTEEL_USE)
            level!!.setBlock(blockPos, blockState.setValue(BlockStateProperties.LIT, true), 11)
            level!!.gameEvent(pPlayer, GameEvent.BLOCK_CHANGE, blockPos)
            pStack.hurtAndBreak(1, pPlayer, LivingEntity.getSlotForHand(pHand))

            return ItemInteractionResult.SUCCESS
        }
        if (!fluidTank.fluidStorage.isFull()) {
            if (pStack.`is`(Items.WATER_BUCKET)) {
                playSound(level, pPlayer, blockPos, SoundEvents.BUCKET_FILL)
                fluidTank.fluidStorage.setFluidStack(FluidStack.create(Fluids.WATER, FluidStack.bucketAmount()))

                setChanged()
                return ItemInteractionResult.SUCCESS
            }

            if (pStack.`is`(Items.POTION)) {
                val potionContents = pStack.get(DataComponents.POTION_CONTENTS)
                if (potionContents?.`is`(Potions.WATER) == true) {
                    playSound(level, pPlayer, blockPos, SoundEvents.BUCKET_FILL)
                    if (!pPlayer.isCreative) {
                        pPlayer.setItemInHand(pHand, Items.GLASS_BOTTLE.defaultInstance)
                    }
                    val currentFluidAmount = fluidTank.fluidStorage.getAmount()
                    fluidTank.fluidStorage.setFluidStack(FluidStack.create(Fluids.WATER, currentFluidAmount + FluidStack.bucketAmount() / 3))
                    setChanged()
                    return ItemInteractionResult.SUCCESS
                }
            }
        }
        if (pStack.`is`(Items.GLASS_BOTTLE)) {

            if (!brewItemOutput.isEmpty && fluidTank.fluidStorage.getAmount() >= (FluidStackHooks.bucketAmount() / 3)) {
                pStack.shrink(1)
                Containers.dropItemStack(level, pPlayer.x, pPlayer.y, pPlayer.z, ItemStack(brewItemOutput.copy().item))
                fluidTank.fluidStorage.remove(FluidStackHooks.bucketAmount() / 3, false)
                playSound(level, pPlayer, blockPos, SoundEvents.ITEM_PICKUP, 0.5f)
                playSound(level, pPlayer, blockPos, SoundEvents.BUCKET_EMPTY)
                if (fluidTank.fluidStorage.getAmount() < (FluidStackHooks.bucketAmount() / 3)) {
                    fullReset()
                }
                setChanged()

                return ItemInteractionResult.SUCCESS
            }
        }

        return super.onUseWithItem(pPlayer, pStack, pHand)
    }

    private fun playSound(level: Level?, player: Player, blockPos: BlockPos, sound: SoundEvent, volume: Float = 1.0f) {
        level!!.playSound(
            player,
            blockPos,
            sound,
            SoundSource.BLOCKS,
            volume,
            level.getRandom().nextFloat() * 0.4f + 0.8f
        )
    }

    private fun spawnSmokeParticle(level: Level, pos: BlockPos) {
        WitcheryPayloads.sendToPlayers(level, pos, CauldronPoofS2CPacket(pos, color))
    }

    private fun getFirstEmptyIndex(): Int {
        for (i in 0 until containerSize) {
            if (getItem(i).isEmpty) {
                return i
            }
        }
        return -1
    }

    override fun loadAdditional(pTag: CompoundTag, pRegistries: HolderLookup.Provider) {
        super.loadAdditional(pTag, pRegistries)
        fluidTank.loadFluidAdditional(pTag, pRegistries)
        craftingProgressTicker = pTag.getInt("craftingProgressTicker")
        color = pTag.getInt("color")
        complete = pTag.getBoolean("complete")
        if (pTag.contains("Item", 10)) {
            val compoundTag: CompoundTag = pTag.getCompound("Item")
            brewItemOutput = ItemStack.parse(pRegistries, compoundTag).orElse(ItemStack.EMPTY) as ItemStack
        } else {
            brewItemOutput = ItemStack.EMPTY
        }
        this.inputItems = NonNullList.withSize(this.containerSize, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(pTag, inputItems, pRegistries)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        fluidTank.saveFluidAdditional(tag, registries)
        tag.putInt("craftingProgressTicker", craftingProgressTicker)
        tag.putInt("color", color)
        tag.putBoolean("complete", complete)
        if (!brewItemOutput.isEmpty) {
            tag.put("Item", brewItemOutput.save(registries))
        }
        ContainerHelper.saveAllItems(tag, inputItems, registries)
    }

    //INVENTORY IMPL
    override fun clearContent() {
        inputItems.clear()
    }

    override fun getContainerSize(): Int {
        return inputItems.size
    }

    override fun isEmpty(): Boolean {
        return inputItems.isEmpty()
    }

    override fun getItem(slot: Int): ItemStack {
        return inputItems[slot]
    }

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        return ContainerHelper.removeItem(inputItems, slot, amount)
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        return ContainerHelper.takeItem(inputItems, slot)
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        inputItems[slot] = stack
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }

    companion object {
        const val WATER_COLOR = 0x3f76e4
        const val PROGRESS_TICKS = 20 * 3

        private fun isOrderRight(inputItems: List<ItemStack>, recipeItems: List<IngredientWithColor>?): Boolean {
            if (recipeItems == null) return false

            // Check if the number of input items is larger than the recipe items
            if (inputItems.size > recipeItems.size) return false

            // Iterate through the input items
            for (index in inputItems.indices) {
                val inputItem = inputItems[index]

                // Check if the corresponding recipe item order matches the index
                val recipeItem = recipeItems.find { it.order == index }

                // If there's no recipe item at this order or the input item doesn't match the ingredient, return false
                if (recipeItem == null || !recipeItem.ingredient.test(inputItem)) {
                    return false
                }
            }

            // If all items match, return true
            return true
        }
    }
}