package dev.sterner.witchery.handler

import com.mojang.blaze3d.platform.ScreenManager.clamp
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.EntityEvent
import dev.architectury.event.events.common.InteractionEvent
import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.event.events.common.TickEvent
import dev.architectury.networking.NetworkManager
import dev.sterner.witchery.Witchery
import dev.sterner.witchery.api.RenderUtils
import dev.sterner.witchery.api.VillagerTransfix
import dev.sterner.witchery.api.multiblock.MultiBlockComponentBlockEntity
import dev.sterner.witchery.api.multiblock.MultiBlockStructure.StructurePiece
import dev.sterner.witchery.block.sacrificial_circle.SacrificialBlock
import dev.sterner.witchery.data.BloodPoolHandler
import dev.sterner.witchery.payload.SpawnBloodParticlesS2CPayload
import dev.sterner.witchery.payload.VampireAbilityUseC2SPayload
import dev.sterner.witchery.platform.transformation.BloodPoolLivingEntityAttachment
import dev.sterner.witchery.platform.transformation.TransformationPlayerAttachment
import dev.sterner.witchery.platform.transformation.VampirePlayerAttachment
import dev.sterner.witchery.platform.transformation.VampirePlayerAttachment.getData
import dev.sterner.witchery.platform.transformation.VampirePlayerAttachment.setData
import dev.sterner.witchery.registry.*
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.Chicken
import net.minecraft.world.entity.monster.Blaze
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import java.util.function.Consumer

object VampireHandler {

    private val overlay = Witchery.id("textures/gui/ability_hotbar_selection.png")
    private val sun = Witchery.id("textures/gui/vampire_abilities/sun_")
    private var abilityIndex = -1 // -1 means player is in the hotbar, not abilities
    private var bloodTransferAmount = 10
    private var bloodThreshold = 75


    fun scroll(minecraft: Minecraft?, x: Double, y: Double): EventResult? {
        val player = minecraft?.player
        if (minecraft == null || player == null) {
            return EventResult.pass()
        }

        val abilityCount = VampirePlayerAttachment.getAbilities(player).size
        if (abilityCount == 0) {
            return EventResult.pass()
        }

        if (abilityIndex == -1) {
            if (player.inventory.selected == 0 && y > 0.0) {
                abilityIndex = 0
                VampirePlayerAttachment.setAbilityIndex(player, abilityIndex)
                return EventResult.interruptFalse()
            } else if (player.inventory.selected == 8 && y < 0.0) {
                abilityIndex = abilityCount - 1
                VampirePlayerAttachment.setAbilityIndex(player, abilityIndex)
                return EventResult.interruptFalse()
            }
        } else {
            if (y > 0.0) {
                if (abilityIndex < abilityCount - 1) {
                    abilityIndex++
                    VampirePlayerAttachment.setAbilityIndex(player, abilityIndex)
                } else {
                    player.inventory.selected = 8
                    abilityIndex = -1
                    VampirePlayerAttachment.setAbilityIndex(player, abilityIndex)
                }
                return EventResult.interruptFalse()
            } else if (y < 0.0) {
                if (abilityIndex > 0) {
                    abilityIndex--
                    VampirePlayerAttachment.setAbilityIndex(player, abilityIndex)
                } else {
                    player.inventory.selected = 0
                    abilityIndex = -1
                    VampirePlayerAttachment.setAbilityIndex(player, abilityIndex)
                }
                return EventResult.interruptFalse()
            }
        }

        return EventResult.pass()
    }


    fun handleHurt(player: LivingEntity, damageSource: DamageSource, original: Float): Float {

        if (player !is Player) {
            return original
        }

        val vampData = getData(player)
        if (vampData.vampireLevel < 1) {
            return original
        }

        val bloodData = BloodPoolLivingEntityAttachment.getData(player)
        if (bloodData.bloodPool > 0) {
            val bloodPerHealthPoint = bloodThreshold
            val maxBloodAbsorbableDamage = bloodData.bloodPool / bloodPerHealthPoint

            val absorbableDamage = minOf(original * 0.75f, maxBloodAbsorbableDamage.toFloat())
            val bloodRequired = (absorbableDamage * bloodPerHealthPoint).toInt()

            BloodPoolLivingEntityAttachment.setData(player, bloodData.copy(bloodPool = bloodData.bloodPool - bloodRequired))

            return original * 0.25f
        }

        return original
    }

    fun tick(player: Player?) {
        if (player == null || player.level().isClientSide) {
            return
        }

        val vampData = getData(player)
        if (vampData.vampireLevel < 1) {
            return
        }

        if (player.isAlive) {
            val isInSunlight = player.level().canSeeSky(player.blockPosition()) && player.level().isDay
            val sunDamageSource = player.level().damageSources().source(WitcheryDamageSources.IN_SUN)
            val bloodData = BloodPoolLivingEntityAttachment.getData(player)

            if (isInSunlight) {
                VampirePlayerAttachment.increaseInSunTick(player)

                if (getData(player).inSunTick >= 100) {
                    if (vampData.vampireLevel < 5) {
                        player.hurt(sunDamageSource, Float.MAX_VALUE)
                    } else {
                        if (bloodData.bloodPool >= bloodThreshold) {
                            if (player.tickCount % 20 == 0) {
                                player.hurt(sunDamageSource, 2f)
                                player.level().playSound(null, player.x, player.y, player.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS , 0.5f, 1.0f)
                                WitcheryPayloads.sendToPlayers(player.level(), SpawnBloodParticlesS2CPayload(player, player.position().add(0.5, 0.5, 0.5)))
                            }
                        } else {
                            player.hurt(sunDamageSource, Float.MAX_VALUE)
                        }
                    }
                }
            } else {
                VampirePlayerAttachment.decreaseInSunTick(player)
            }

            if (bloodData.bloodPool >= 75 && player.level().random.nextBoolean()) {
                if (player.health < player.maxHealth && player.health > 0) {
                    BloodPoolLivingEntityAttachment.decreaseBlood(player, 75)
                    player.heal(1f)
                }
            }

            if (getData(player).isNightVisionActive) {
                player.addEffect(MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 2))
            }
            if (getData(player).isSpeedBoostActive) {
                player.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 2))
            }
        }
    }


    @JvmStatic
    fun interactEntity(player: Player?, entity: Entity?, interactionHand: InteractionHand?): EventResult? {
        if (player is ServerPlayer && entity is LivingEntity) {
            val playerData = getData(player)
            val playerBloodData = BloodPoolLivingEntityAttachment.getData(player)
            if (playerData.abilityIndex == VampirePlayerAttachment.VampireAbility.DRINK_BLOOD.ordinal) {
                val targetData = BloodPoolLivingEntityAttachment.getData(entity)
                val quality = BloodPoolHandler.BLOOD_PAIR[entity.type] ?: 0

                if (playerBloodData.bloodPool < playerBloodData.maxBlood && targetData.bloodPool >= 0 && targetData.maxBlood > 0) {

                    if (quality == 0 && player.level().random.nextFloat() < 0.25f) {
                        player.addEffect(MobEffectInstance(MobEffects.POISON, 200, 0))
                    }

                    val targetHalfBlood = targetData.maxBlood / 2

                    if (targetData.bloodPool <= targetHalfBlood && !player.isShiftKeyDown && entity is Villager && getData(player).vampireLevel == 2) {
                        VampirePlayerAttachment.increaseVillagersHalfBlood(player, entity)
                    }

                    if (targetData.bloodPool <= targetHalfBlood && !player.isShiftKeyDown) {
                        return EventResult.pass()
                    }

                    player.level().playSound(null, entity.x, entity.y, entity.z, SoundEvents.HONEY_DRINK, SoundSource.PLAYERS)
                    val np = entity.position().add(0.5, 0.5, 0.5)
                    WitcheryPayloads.sendToPlayers(player.level(), SpawnBloodParticlesS2CPayload(player, np))

                    if (getData(player).vampireLevel == 2 && entity is Villager) {
                        VampirePlayerAttachment.increaseVillagersHalfBlood(player, entity)
                    }

                    BloodPoolLivingEntityAttachment.decreaseBlood(entity, bloodTransferAmount)
                    BloodPoolLivingEntityAttachment.increaseBlood(player, bloodTransferAmount)

                    val shouldHurt = when {
                        entity is Villager && entity is VillagerTransfix && !entity.isSleeping && !entity.isTransfixed() -> true
                        targetData.bloodPool < targetHalfBlood -> true
                        else -> false
                    }

                    if (shouldHurt) {
                        if (entity is Villager && getData(player).villagersHalfBlood.contains(entity.uuid)) {
                            VampirePlayerAttachment.removeVillagerHalfBlood(player, entity)
                        }
                        entity.hurt(player.damageSources().playerAttack(player), 2f)
                    }

                    if (targetData.bloodPool <= 0) {
                        entity.kill()
                    }

                    return EventResult.interruptFalse()
                }
                return EventResult.interruptFalse()
            } else if (playerData.abilityIndex == VampirePlayerAttachment.VampireAbility.TRANSFIX.ordinal) {
                if (entity is Villager) {
                    val transfixVillager = entity as VillagerTransfix
                    transfixVillager.setTransfixedLookVector(player.eyePosition)
                    return EventResult.interruptFalse()
                }
            }
        }
        return EventResult.pass()
    }

    @JvmStatic
    fun renderHud(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {

        val client = Minecraft.getInstance()
        val player = client.player ?: return

        val isNotVamp = getData(player).vampireLevel <= 0

        if (isNotVamp) {
            return
        }

        val abilityIndex = getData(player).abilityIndex
        val size = VampirePlayerAttachment.getAbilities(player)

        val y = guiGraphics.guiHeight() - 18 - 5
        val x = guiGraphics.guiWidth() / 2 - 36 - 18 * 4 - 5

        drawBloodSense(guiGraphics)
        drawSun(guiGraphics, player)

        val bl = player.isShiftKeyDown

        for (i in size.indices) {
            var name = size[i].serializedName
            if (size[i] == VampirePlayerAttachment.VampireAbility.TRANSFIX && bl) {
                name = "night_vision"
            }
            guiGraphics.blit(Witchery.id("textures/gui/vampire_abilities/${name}.png"), x - (25 * i) + 4, y + 4, 16, 16, 0f,0f,16, 16,16, 16)
        }

        if (abilityIndex != -1) {
            guiGraphics.blit(overlay, x - (25 * abilityIndex), y, 24, 23, 0f,0f,24, 23,24, 23)
        }
    }

    private fun drawSun(guiGraphics: GuiGraphics, player: Player) {
        val y = guiGraphics.guiHeight() - 36 - 18 - 2
        val x = guiGraphics.guiWidth() / 2 - 8
        val raw = getData(player).inSunTick
        val sunTick = clamp((raw / 20) - 1, 0, 4)
        if (raw > 1) {
            RenderUtils.blitWithAlpha(guiGraphics.pose(), sun.withSuffix("${sunTick}.png"), x, y, 0f,0f,16,16,16,16)
        }
    }

    private fun drawBloodSense(guiGraphics: GuiGraphics) {
        val x = guiGraphics.guiWidth() / 2 + 13
        val y = guiGraphics.guiHeight() / 2 + 9
        val target = Minecraft.getInstance().crosshairPickEntity
        if (target is LivingEntity && BloodPoolLivingEntityAttachment.getData(target).maxBlood > 0) {
            RenderUtils.innerRenderBlood(guiGraphics,target, y, x)
        }
    }

    fun respawn(oldPlayer: ServerPlayer, newPlayer: ServerPlayer, b: Boolean) {
        if (getData(oldPlayer).vampireLevel > 0) {
            val oldBloodData = BloodPoolLivingEntityAttachment.getData(oldPlayer)
            newPlayer.foodData.foodLevel = 10
            BloodPoolLivingEntityAttachment.setData(newPlayer, BloodPoolLivingEntityAttachment.Data(oldBloodData.maxBlood, 900))
            val data = getData(oldPlayer).copy(inSunTick = 0)
            setData(newPlayer, data)
        }
    }

    private fun clientRightClick(player: Player?, interactionHand: InteractionHand?) {
        if (player == null || interactionHand == InteractionHand.OFF_HAND) {
            return
        }

        val playerData = getData(player)
        NetworkManager.sendToServer(VampireAbilityUseC2SPayload(playerData.abilityIndex))
    }

    private fun rightClickBlock(player: Player?, interactionHand: InteractionHand?, blockPos: BlockPos?, direction: Direction?): EventResult? {

        if (player == null || player.level().isClientSide || interactionHand == InteractionHand.OFF_HAND) {
            return EventResult.pass()
        }

        val playerData = getData(player)
        if (playerData.abilityIndex == VampirePlayerAttachment.VampireAbility.TRANSFIX.ordinal && player.isShiftKeyDown) {
            VampirePlayerAttachment.toggleNightVision(player)
            return EventResult.interruptTrue()
        } else if (playerData.abilityIndex == VampirePlayerAttachment.VampireAbility.SPEED.ordinal) {
            VampirePlayerAttachment.toggleSpeedBoost(player)
            return EventResult.interruptTrue()
        } else if (playerData.abilityIndex == VampirePlayerAttachment.VampireAbility.BAT_FORM.ordinal) {
            val isBta = TransformationPlayerAttachment.isBat(player)
            if (isBta) {
                TransformationPlayerAttachment.removeForm(player)
            } else {
                TransformationPlayerAttachment.setBatForm(player)
            }
            return EventResult.interruptTrue()
        }

        return EventResult.pass()
    }

    private fun killChicken(livingEntity: LivingEntity?, damageSource: DamageSource?): EventResult? {
        if (livingEntity is Chicken && damageSource?.entity is Player) {
            val player = damageSource.entity as Player

            val wineOrBottle = player.offhandItem.`is`(WitcheryItems.WINE_GLASS.get()) || player.offhandItem.`is`(Items.GLASS_BOTTLE)

            if (player.mainHandItem.`is`(WitcheryItems.ARTHANA.get()) && wineOrBottle) {

                val possibleSkull = BlockPos.betweenClosedStream(livingEntity.boundingBox.inflate(2.0))

                var shouldPerform = false
                for (skullPos in possibleSkull) {
                    val skullState = livingEntity.level().getBlockState(skullPos)

                    if (skullState.`is`(Blocks.SKELETON_SKULL)
                        || skullState.`is`(Blocks.SKELETON_WALL_SKULL)
                        || skullState.`is`(Blocks.WITHER_SKELETON_SKULL)
                        || skullState.`is`(Blocks.WITHER_SKELETON_WALL_SKULL)) {

                        if (hasRitualStructure(livingEntity.level(), skullPos)) {
                            shouldPerform = true
                            break
                        }
                    }
                }

                if (shouldPerform) {
                    player.offhandItem.shrink(1)
                    val stackCopy = player.offhandItem.copy()
                    if (!stackCopy.isEmpty) {
                        Containers.dropItemStack(livingEntity.level(), player.x, player.y, player.z, stackCopy)
                    }

                    val bloodWine = WitcheryItems.WINE_GLASS.get().defaultInstance
                    bloodWine.set(WitcheryDataComponents.CHICKEN_BLOOD.get(), true)
                    bloodWine.set(WitcheryDataComponents.BLOOD.get(), livingEntity.uuid)
                    player.setItemInHand(InteractionHand.OFF_HAND, bloodWine)
                }
            }
        }

        return EventResult.pass()
    }

    private fun hasRitualStructure(level: Level, skullPos: BlockPos?): Boolean {
        //TODO? maybe always true is fine
        return true
    }

    fun makeSacrificialCircle(player: Player, blockPos: BlockPos): EventResult? {
        val pieces = SacrificialBlock.STRUCTURE.get().structurePieces

        pieces.forEach(Consumer { s: StructurePiece ->
            s.place(
                blockPos,
                player.level()
            )
        })

        player.level().setBlockAndUpdate(
            blockPos,
            WitcheryBlocks.SACRIFICIAL_CIRCLE.get().defaultBlockState()
        )

        if (player.level().getBlockEntity(blockPos) is MultiBlockComponentBlockEntity) {
            (player.level().getBlockEntity(blockPos) as MultiBlockComponentBlockEntity).corePos = blockPos
        }

        return EventResult.pass()
    }

    fun tickNightsCount(player: Player?) {
        if (player is ServerPlayer) {
            if (getData(player).vampireLevel == 3) {
                VampirePlayerAttachment.increaseNightTicker(player)

                if (getData(player).nightTicker >= 24000 * 4 && player.level().isNight) {
                    VampirePlayerAttachment.increaseVampireLevel(player)
                }
            }
        }
    }

    fun resetNightCount(livingEntity: LivingEntity?, damageSource: DamageSource?): EventResult? {
        if (livingEntity is Player && getData(livingEntity).vampireLevel == 3) {
            VampirePlayerAttachment.resetNightCounter(livingEntity)
        }

        return EventResult.pass()
    }

    fun killBlaze(livingEntity: LivingEntity?, damageSource: DamageSource?): EventResult? {
        if (livingEntity is Blaze && damageSource?.entity is ServerPlayer) {
            val player = damageSource.entity as ServerPlayer

            if (getData(player).vampireLevel == 5) {
                VampirePlayerAttachment.increaseKilledBlazes(player)
            }
        }

        return EventResult.pass()
    }

    fun registerEvents() {
        TickEvent.PLAYER_PRE.register(VampireHandler::tickNightsCount)
        EntityEvent.LIVING_DEATH.register(VampireHandler::resetNightCount)
        InteractionEvent.INTERACT_ENTITY.register(VampireHandler::interactEntity)
        InteractionEvent.CLIENT_RIGHT_CLICK_AIR.register(VampireHandler::clientRightClick)
        InteractionEvent.RIGHT_CLICK_BLOCK.register(VampireHandler::rightClickBlock)
        EntityEvent.LIVING_DEATH.register(VampireHandler::killChicken)
        EntityEvent.LIVING_DEATH.register(VampireHandler::killBlaze)
        TickEvent.PLAYER_PRE.register(VampireHandler::tick)
        PlayerEvent.PLAYER_CLONE.register(VampireHandler::respawn)
    }
}