# This Bukkit plugin adds custom crafting recipes
# which are listed in the method 'addRecipes'
#
# Use RubyBukkit to load Ruby-based plugins
# <http://forums.bukkit.org/threads/RubyBukkit.24899/>

Plugin.is {
    name "CustomRecipes"
    version "0.1"
    author "Zeerix"
}

import 'org.bukkit.Material'
import 'org.bukkit.inventory.ItemStack'
import 'org.bukkit.inventory.ShapedRecipe'
import 'org.bukkit.inventory.ShapelessRecipe'

class CustomRecipes < RubyPlugin
    def onEnable
        @recipes = []
        addRecipes
        @recipes.each { |recipe| getServer.addRecipe recipe }
        print "[" + description.name + "] " + description.fullName + " enabled."
    end
    def onDisable; end
    
    def addShaped(*recipe)
        @recipes << ShapedRecipe.new(ItemStack.new(*recipe)); @recipes.last
    end
    def addShapeless(*recipe)
        @recipes << ShapelessRecipe.new(ItemStack.new(*recipe)); @recipes.last
    end    

    def addRecipes
        # add 2*STEP <=> DOUBLE_STEP
        # makes it easier to build floors of double steps
        (0..6).each { |data|
            addShaped(Material::DOUBLE_STEP, 1, data).shape("-", "-").setIngredient(?-, Material::STEP, data)
            addShapeless(Material::STEP, 2, data).addIngredient(1, Material::DOUBLE_STEP, data)
        }

        # stack of 3 sugar cane => book
        # this is a shortcut for the tedious book crafting
        addShaped(Material::BOOK, 1, 0).shape("-", "-", "-").setIngredient(?-, Material::SUGAR_CANE, 0)

        # 6 stone blocks to craft the uncrafter step 44:6
        # they look similar to the normal step/double-steps but have a different side-texture
        addShaped(Material::STEP, 6, 6).shape("---", "---").setIngredient(?-, Material::STONE, 0)
    end
end
