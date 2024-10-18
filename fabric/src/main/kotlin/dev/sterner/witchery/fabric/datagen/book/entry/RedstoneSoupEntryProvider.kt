package dev.sterner.witchery.fabric.datagen.book.entry

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase
import com.klikli_dev.modonomicon.api.datagen.EntryBackground
import com.klikli_dev.modonomicon.api.datagen.EntryProvider
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel
import com.klikli_dev.modonomicon.api.datagen.book.page.BookRecipePageModel
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel
import com.mojang.datafixers.util.Pair
import dev.sterner.witchery.Witchery
import dev.sterner.witchery.fabric.datagen.book.page.BookCauldronBrewingPageModel
import dev.sterner.witchery.fabric.datagen.book.page.BookCauldronCraftingPageModel
import dev.sterner.witchery.integration.modonomicon.BookCauldronBrewingRecipePage
import dev.sterner.witchery.recipe.cauldron.CauldronCraftingRecipe
import dev.sterner.witchery.registry.WitcheryItems
import net.minecraft.world.item.Items

class RedstoneSoupEntryProvider(parent: CategoryProviderBase?) : EntryProvider(parent) {

    companion object {
        val ID = "redstone_soup"
    }

    override fun generatePages() {
        this.page(ID) {
            BookTextPageModel.create()
                .withTitle("$ID.title")
                .withText("$ID.page1")
        }

        this.page("ID") {
            BookCauldronBrewingPageModel.create().withText("$ID.title")
                .withRecipeId1(Witchery.id("cauldron_brewing/redstone_soup"));
        }


    }

    override fun entryName(): String {
        return ID.replaceFirstChar { it.uppercaseChar() }
    }

    override fun entryDescription(): String {
        return ""
    }

    override fun entryBackground(): Pair<Int, Int> {
        return EntryBackground.DEFAULT
    }

    override fun entryIcon(): BookIconModel {
        return BookIconModel.create(WitcheryItems.REDSTONE_SOUP.get())
    }

    override fun entryId(): String {
        return ID
    }
}