package com.willfp.ecoenchants.util.internal.drops;

import com.willfp.ecoenchants.enchantments.EcoEnchants;
import com.willfp.ecoenchants.enchantments.util.EnchantChecks;
import com.willfp.ecoenchants.util.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class InternalDropQueue implements AbstractDropQueue {
    protected final List<ItemStack> items;
    protected int xp;
    protected final Player player;
    protected Location loc;
    protected boolean hasTelekinesis = false;
    protected ItemStack item;

    private static boolean useOrb;

    /**
     * Create {@link com.willfp.ecoenchants.util.internal.drops.InternalDropQueue} linked to player
     *
     * @param player The player
     */
    public InternalDropQueue(Player player) {
        this.items = new ArrayList<>();
        this.xp = 0;
        this.player = player;
        this.loc = player.getLocation();
        this.item = player.getInventory().getItemInMainHand();
    }

    /**
     * Add item to queue
     *
     * @param item The item to add
     *
     * @return The DropQueue
     */
    @Override
    public AbstractDropQueue addItem(ItemStack item) {
        this.items.add(item);
        return this;
    }

    /**
     * Add multiple items to queue
     *
     * @param itemStacks The items to add
     *
     * @return The DropQueue
     */
    @Override
    public AbstractDropQueue addItems(Collection<ItemStack> itemStacks) {
        this.items.addAll(itemStacks);
        return this;
    }

    /**
     * Add xp to queue
     *
     * @param amount The amount to add
     *
     * @return The DropQueue
     */
    @Override
    public AbstractDropQueue addXP(int amount) {
        this.xp += amount;
        return this;
    }

    /**
     * Set location of the origin of the drops
     *
     * @param l The location
     *
     * @return The DropQueue
     */
    @Override
    public AbstractDropQueue setLocation(Location l) {
        this.loc = l;
        return this;
    }

    /**
     * Force the queue to act as if player has {@link EcoEnchants#TELEKINESIS}
     *
     * @return The DropQueue
     */
    @Override
    public AbstractDropQueue forceTelekinesis() {
        this.hasTelekinesis = true;
        return this;
    }

    /**
     * Set the queue to test specific item for {@link EcoEnchants#TELEKINESIS}
     * Default item is the player's held item, however for this is required for Tridents.
     *
     * @param item The item to test
     *
     * @return The DropQueue
     */
    @Override
    public AbstractDropQueue setItem(ItemStack item) {
        this.item = item;
        return this;
    }

    /**
     * Push the queue
     */
    public void push() {
        if(!hasTelekinesis) hasTelekinesis = EnchantChecks.item(item, EcoEnchants.TELEKINESIS);
        if(hasTelekinesis && !EcoEnchants.TELEKINESIS.isEnabled()) hasTelekinesis = false;

        World world = loc.getWorld();
        assert world != null;

        if(hasTelekinesis) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(items.toArray(new ItemStack[]{}));
            for(ItemStack drop : leftover.values()) {
                world.dropItemNaturally(loc.add(0.5, 0, 0.5), drop).setVelocity(new Vector());
            }
            if (xp > 0) {
                PlayerExpChangeEvent event = new PlayerExpChangeEvent(player, xp);
                Bukkit.getPluginManager().callEvent(event);
                if (useOrb) {
                    ExperienceOrb orb = (ExperienceOrb) world.spawnEntity(player.getLocation().add(0, 0.2, 0), EntityType.EXPERIENCE_ORB);
                    orb.setVelocity(new Vector(0, 0, 0));
                    orb.setExperience(event.getAmount());
                } else {
                    player.giveExp(event.getAmount());
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 1f, (float) NumberUtils.randFloat(0.7, 1.2));
                }
            }
        } else {
            for (ItemStack drop : items) {
                world.dropItemNaturally(loc.add(0.5, 0, 0.5), drop).setVelocity(new Vector());
            }
            if (xp > 0) {
                ExperienceOrb orb = (ExperienceOrb) world.spawnEntity(loc, EntityType.EXPERIENCE_ORB);
                orb.setExperience(xp);
            }
        }
    }

    public static void update() {
        useOrb = EcoEnchants.TELEKINESIS.getConfig().getBool(EcoEnchants.CONFIG_LOCATION + "use-orb");
    }

    static {
        update();
    }
}