package dev.sterner.witchery.registry

import dev.architectury.registry.registries.DeferredRegister
import dev.sterner.witchery.Witchery
import dev.sterner.witchery.entity.FloatingItemEntity
import dev.sterner.witchery.entity.MandrakeEntity
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.Level

object WitcheryEntityTypes {

    val ENTITY_TYPES: DeferredRegister<EntityType<*>> = DeferredRegister.create(Witchery.MODID, Registries.ENTITY_TYPE)

    val MANDRAKE = ENTITY_TYPES.register("mandrake") {
        EntityType.Builder.of(
            { _: EntityType<MandrakeEntity>, level: Level ->
                MandrakeEntity(level)
            }, MobCategory.CREATURE
        ).sized(0.5f, 0.5f).build(Witchery.id("mandrake").toString())
    }


    val FLOATING_ITEM =
        ENTITY_TYPES.register(
            "floating_item"
        ) {
            EntityType.Builder.of(
                { _: EntityType<FloatingItemEntity>, w: Level ->
                    FloatingItemEntity(
                        w
                    )
                }, MobCategory.MISC
            ).sized(0.5f, 0.75f).clientTrackingRange(10)
                .build(Witchery.id("floating_item").toString())
        }
}