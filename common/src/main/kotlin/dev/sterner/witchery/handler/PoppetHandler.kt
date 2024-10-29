package dev.sterner.witchery.handler

import dev.architectury.event.EventResult
import dev.sterner.witchery.item.TaglockItem
import dev.sterner.witchery.item.TaglockItem.Companion.getLivingEntity
import dev.sterner.witchery.item.TaglockItem.Companion.getPlayer
import dev.sterner.witchery.mixin.ItemEntityMixin
import dev.sterner.witchery.platform.poppet.PoppetDataAttachment
import dev.sterner.witchery.platform.poppet.VoodooPoppetData
import dev.sterner.witchery.platform.poppet.VoodooPoppetDataAttachment
import dev.sterner.witchery.registry.WitcheryDataComponents
import dev.sterner.witchery.registry.WitcheryItems
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

object PoppetHandler {

    fun deathProtectionPoppet(livingEntity: LivingEntity?, damageSource: DamageSource?): EventResult? {
        if (livingEntity is Player) {
            if (deathProtectionHelper(livingEntity, damageSource)) {
                return EventResult.interruptFalse()
            }
        }
        return EventResult.pass()
    }

    private fun deathProtectionHelper(player: Player, damageSource: DamageSource?): Boolean {
        if (damageSource != null && damageSource.`is`(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false
        } else {
            val itemStack: ItemStack? = consumePoppet(player, WitcheryItems.DEATH_PROTECTION_POPPET.get())

            if (itemStack != null) {
                player.health = 4.0f
                player.removeAllEffects()
                player.addEffect(MobEffectInstance(MobEffects.REGENERATION, 900, 1))
                player.addEffect(MobEffectInstance(MobEffects.ABSORPTION, 100, 1))
                player.addEffect(MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0))
                player.playSound(SoundEvents.TOTEM_USE)
            }

            return itemStack != null
        }
    }

    fun hasArmorProtectionPoppet(level: ServerLevel, player: ServerPlayer?): Boolean {
        val playerPoppet = player?.let { consumePoppet(it, WitcheryItems.ARMOR_PROTECTION_POPPET.get()) }
        if (playerPoppet != null) return true

        val poppetData = PoppetDataAttachment.getPoppetData(level)
        return poppetData.poppetDataMap.any { data ->
            data.poppetItemStack.`is`(WitcheryItems.ARMOR_PROTECTION_POPPET.get()) &&
                    isPoppetBoundToLiving(data.poppetItemStack, player)
        }
    }

    private fun isPoppetBoundToLiving(itemStack: ItemStack, livingEntity: LivingEntity?): Boolean {
        return if (livingEntity is Player) {
            val profile = itemStack.get(DataComponents.PROFILE)
            profile?.gameProfile == livingEntity.gameProfile
        } else {
            itemStack.get(WitcheryDataComponents.ENTITY_ID_COMPONENT.get()) == livingEntity?.stringUUID
        }
    }

    fun hungerProtectionPoppet(livingEntity: LivingEntity?, damageSource: DamageSource?): EventResult? {
        if (livingEntity is Player) {
            if (hungerProtectionPoppetHelper(livingEntity, damageSource)) {
                return EventResult.interruptFalse()
            }
        }
        return EventResult.pass()
    }

    private fun hungerProtectionPoppetHelper(livingEntity: LivingEntity, damageSource: DamageSource?): Boolean {
        if (livingEntity is Player && damageSource != null && damageSource.`is`(DamageTypes.STARVE)) {
            val itemStack: ItemStack? = consumePoppet(livingEntity, WitcheryItems.HUNGER_PROTECTION_POPPET.get())

            if (itemStack != null) {
                livingEntity.health = 10.0f
                livingEntity.foodData.foodLevel = 20
                livingEntity.removeAllEffects()
                livingEntity.level().broadcastEntityEvent(livingEntity, 35.toByte())
            }

            return itemStack != null
        }
        return false
    }

    private fun consumePoppet(livingEntity: LivingEntity, item: Item): ItemStack? {
        var itemStack: ItemStack?
        var consume: Boolean

        val (accessoryConsume, accessoryItem) = AccessoryHandler.check(livingEntity, item)
        itemStack = accessoryItem
        consume = accessoryConsume

        if (!consume) {
            for (interactionHand in InteractionHand.entries) {
                val itemStack2: ItemStack = livingEntity.getItemInHand(interactionHand)
                if (itemStack2.`is`(item) && isPoppetBoundToLiving(itemStack2, livingEntity)) {
                    itemStack = itemStack2.copy()
                    itemStack2.shrink(1)
                    consume = true
                    break
                }
            }
        }

        if (!consume && livingEntity.level() is ServerLevel) {
            val level = livingEntity.level() as ServerLevel
            val poppetData = PoppetDataAttachment.getPoppetData(level)

            val blockPoppet = poppetData.poppetDataMap.find {
                it.poppetItemStack.`is`(item) && isPoppetBoundToLiving(it.poppetItemStack, livingEntity)
            }

            if (blockPoppet != null) {
                itemStack = blockPoppet.poppetItemStack.copy()
                blockPoppet.poppetItemStack.shrink(1)
                PoppetDataAttachment.updatePoppetItem(level, blockPoppet.blockPos, blockPoppet.poppetItemStack)
            }
        }

        return itemStack
    }

    fun handleVampiricPoppet(livingEntity: LivingEntity?, damageSource: DamageSource ,original: Float): Float {
        if (livingEntity != null) {
            var itemStack: ItemStack? = AccessoryHandler.checkNoConsume(livingEntity, WitcheryItems.VAMPIRIC_POPPET.get())

            if (itemStack == null) {
                for (interactionHand in InteractionHand.entries) {
                    val handItem: ItemStack = livingEntity.getItemInHand(interactionHand)
                    if (handItem.`is`(WitcheryItems.VAMPIRIC_POPPET.get())) {
                        itemStack = handItem
                        break
                    }
                }

                if (itemStack == null && livingEntity.level() is ServerLevel) {
                    val level = livingEntity.level() as ServerLevel
                    val poppetData = PoppetDataAttachment.getPoppetData(level)

                    val blockPoppet = poppetData.poppetDataMap.find {
                        it.poppetItemStack.`is`(WitcheryItems.VAMPIRIC_POPPET.get()) &&
                                isPoppetBoundToLiving(it.poppetItemStack, livingEntity)
                    }

                    if (blockPoppet != null) {
                        itemStack = blockPoppet.poppetItemStack.copy()
                        blockPoppet.poppetItemStack.damageValue += 1
                        if (blockPoppet.poppetItemStack.damageValue >= blockPoppet.poppetItemStack.maxDamage) {
                            blockPoppet.poppetItemStack.shrink(1)
                        }
                        PoppetDataAttachment.updatePoppetItem(level, blockPoppet.blockPos, blockPoppet.poppetItemStack)
                    }
                }
            }

            if (itemStack != null) {
                val maybePlayer = TaglockItem.getPlayer(livingEntity.level(), itemStack)
                val maybeEntity = TaglockItem.getLivingEntity(livingEntity.level(), itemStack)
                if (maybePlayer != null || maybeEntity != null) {
                    val halfDamage = original / 2
                    maybePlayer?.hurt(damageSource, halfDamage)
                    maybeEntity?.hurt(damageSource, halfDamage)

                    itemStack.damageValue += 1
                    if (itemStack.damageValue >= itemStack.maxDamage) {
                        itemStack.shrink(1)
                    }
                    return halfDamage
                }
            }
        }

        return original
    }

    fun handleVoodoo(entity: ItemEntity) {
        val movementVector: Vec3 = entity.deltaMovement
        val itemStack = entity.item

        val boundPlayer = getPlayer(entity.level(), itemStack)
        val boundEntity = getLivingEntity(entity.level(), itemStack)

        if (boundPlayer != null || boundEntity != null) {

            if (movementVector.length() > 0.2) {
                val scaledMovement = movementVector.scale(0.45)
                boundPlayer?.apply {
                    addDeltaMovement(scaledMovement)
                    hurtMarked = true
                }
                boundEntity?.apply {
                    addDeltaMovement(scaledMovement)
                    hurtMarked = true
                }
            }

            if (entity.isUnderWater) {
                boundPlayer?.let { VoodooPoppetDataAttachment.setPoppetData(it, VoodooPoppetData(true)) }
                boundEntity?.let { VoodooPoppetDataAttachment.setPoppetData(it, VoodooPoppetData(true)) }
            } else {
                boundPlayer?.let { VoodooPoppetDataAttachment.setPoppetData(it, VoodooPoppetData(false)) }
                boundEntity?.let { VoodooPoppetDataAttachment.setPoppetData(it, VoodooPoppetData(false)) }
            }

            entity.item.damageValue += 1
            if (entity.item.damageValue >= entity.item.maxDamage) {
                entity.remove(Entity.RemovalReason.DISCARDED)
            }
        }
    }

    fun handleUseVoodoo(level: Level, pos: BlockPos, item: ItemStack, player: Player?, blockHitResult: BlockHitResult): InteractionResult {
        if (level.getBlockState(blockHitResult.blockPos).`is`(Blocks.LAVA)) {
            val maybePlayer = getPlayer(level, item)
            val maybeEntity = getLivingEntity(level, item)
            if (maybePlayer != null || maybeEntity != null) {
                maybePlayer?.remainingFireTicks = 20 * 4
                maybeEntity?.remainingFireTicks = 20 * 4
                item.damageValue += 16
                if (item.damageValue >= item.maxDamage) {
                    item.shrink(1)
                }
                return InteractionResult.SUCCESS
            }
        } else if (level.getBlockState(pos).`is`(Blocks.FIRE)) {
            val maybePlayer = getPlayer(level, item)
            val maybeEntity = getLivingEntity(level, item)
            if (maybePlayer != null || maybeEntity != null) {
                maybePlayer?.remainingFireTicks = 20 * 2
                maybeEntity?.remainingFireTicks = 20 * 2
                item.damageValue += 8
                if (item.damageValue >= item.maxDamage) {
                    item.shrink(1)
                }
                return InteractionResult.SUCCESS
            }
        }
        return InteractionResult.PASS
    }
}