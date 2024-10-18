package dev.sterner.witchery.integration.modonomicon

import com.klikli_dev.modonomicon.book.page.BookRecipePage
import com.klikli_dev.modonomicon.client.render.page.BookRecipePageRenderer
import dev.sterner.witchery.Witchery
import dev.sterner.witchery.api.RenderUtils.blitWithAlpha
import dev.sterner.witchery.recipe.cauldron.CauldronCraftingRecipe
import dev.sterner.witchery.registry.WitcheryItems
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Style
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeHolder


abstract class BookCauldronCraftingRecipePageRenderer<T : Recipe<*>?>(page: BookCauldronCraftingRecipePage?) :
    BookRecipePageRenderer<CauldronCraftingRecipe?, BookRecipePage<CauldronCraftingRecipe?>?>(page) {


    override fun getRecipeHeight(): Int {
        return 45
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, ticks: Float) {
        val recipeX = X
        val recipeY = Y

        this.drawRecipe(
            guiGraphics,
            page!!.recipe1, recipeX, recipeY, mouseX, mouseY, false
        )

        val style: Style? = this.getClickedComponentStyleAt(mouseX.toDouble(), mouseY.toDouble())
        if (style != null) parentScreen.renderComponentHoverEffect(guiGraphics, style, mouseX, mouseY)
    }

    override fun getClickedComponentStyleAt(pMouseX: Double, pMouseY: Double): Style? {
        val textStyle = super.getClickedComponentStyleAt(pMouseX, pMouseY)

        return textStyle
    }


    override fun drawRecipe(
        guiGraphics: GuiGraphics,
        recipeHolder: RecipeHolder<CauldronCraftingRecipe?>,
        recipeX: Int,
        recipeY: Int,
        mouseX: Int,
        mouseY: Int,
        second: Boolean
    ) {
        val pose = guiGraphics.pose()

        pose.pushPose()
        pose.translate(-8.0,0.0,0.0)
        // Render input items
        for ((index, ingredient) in recipeHolder.value!!.inputItems.withIndex()) {
            // Draw background texture for each ingredient
            guiGraphics.blit(
                Witchery.id("textures/gui/order_widget.png"),
                recipeX + 2, recipeY + 20 * index,
                0f, 0f,
                48, 18,
                48, 18
            )

            blitWithAlpha(
                pose,
                Witchery.id("textures/gui/index_${ingredient.order + 1}.png"),
                recipeX + 2 + 2, recipeY + 20 * index + 2,
                0f, 0f,
                13, 13,
                13, 13
            )

            // Render the actual item in the slot
            guiGraphics.renderItem(
                ingredient.itemStack,
                recipeX + 2 + 2 + 18,
                recipeY + 20 * index
            )
        }

        // Render output items
        for ((index, itemStack) in recipeHolder.value!!.outputItems.withIndex()) {
            guiGraphics.renderItem(
                itemStack,
                recipeX + 48 + 9 + 4 + 6 + (18 * index),
                recipeY + 20 + 6 - 4 + 18
            )
        }

        // Render the cauldron icon
        guiGraphics.blit(
            Witchery.id("textures/gui/cauldron_modonomicon.png"),
            recipeX + 48 + 9, recipeY + 20 + 18 + 18,
            0f, 0f,
            35, 56,
            35, 56
        )

        // Pop the pose to restore state
        pose.popPose()
    }
}