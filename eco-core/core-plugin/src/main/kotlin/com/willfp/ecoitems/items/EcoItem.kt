package com.willfp.ecoitems.items

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.display.Display
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.recipe.Recipes
import com.willfp.eco.core.registry.Registrable
import com.willfp.ecoitems.slot.ItemSlots
import com.willfp.libreforge.Holder
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.Effects
import org.bukkit.inventory.ItemStack
import java.util.Objects

class EcoItem(
    id: String,
    val config: Config,
    private val plugin: EcoPlugin
) : Holder, Registrable {
    override val effects = Effects.compile(
        config.getSubsections("effects"),
        ViolationContext(plugin, "Item ID $id")
    )

    override val conditions = Conditions.compile(
        config.getSubsections("conditions"),
        ViolationContext(plugin, "Item ID $id")
    )

    override val id = plugin.createNamespacedKey(id)

    val lore: List<String> = config.getStrings("item.lore")

    val displayName: String = config.getString("item.display-name")

    val slot = ItemSlots.getByID(config.getString("slot"))

    // Defensive copy
    private val _itemStack: ItemStack = run {
        val itemConfig = config.getSubsection("item")
        ItemStackBuilder(Items.lookup(itemConfig.getString("item")).item).apply {
            setDisplayName(itemConfig.getFormattedString("display-name"))
            addLoreLines(
                itemConfig.getFormattedStrings("lore").map { "${Display.PREFIX}$it" }
            )
        }.build().apply {
            ecoItem = this@EcoItem
        }
    }

    val itemStack: ItemStack
        get() = _itemStack.clone()

    val effectiveDurability = config.getIntOrNull("item.effective-durability") ?: itemStack.type.maxDurability.toInt()

    val customItem = CustomItem(
        plugin.namespacedKeyFactory.create(id),
        { test -> test.ecoItem == this },
        itemStack
    ).apply { register() }

    val craftingRecipe = if (config.getBool("item.craftable")) {
        Recipes.createAndRegisterRecipe(
            plugin,
            id,
            itemStack,
            config.getStrings("item.recipe"),
            config.getStringOrNull("item.crafting-permission")
        )
    } else null

    val baseDamage = config.getDoubleOrNull("base-damage")

    val baseAttackSpeed = config.getDoubleOrNull("base-attack-speed")

    override fun getID(): String {
        return this.id.key
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EcoItem) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }

    override fun toString(): String {
        return "EcoItem{$id}"
    }
}
