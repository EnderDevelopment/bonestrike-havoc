package com.enderdevelopment.plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Main extends JavaPlugin implements Listener {
    private NamespacedKey maceKey;
    private NamespacedKey playerKillsKey;
    private NamespacedKey upgradedKey;
    private final Map<UUID, Double> fallHeights = new ConcurrentHashMap<>();
    private final Map<UUID, Long> stunnedUntil = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        maceKey = new NamespacedKey(this, "bone_mace");
        playerKillsKey = new NamespacedKey(this, "player_kills");
        upgradedKey = new NamespacedKey(this, "upgraded");
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        fallHeights.clear();
        stunnedUntil.clear();
    }

    private ItemStack createMace() {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text("Bone Handle Mace", NamedTextColor.WHITE));
        meta.lore(List.of(Component.text("A brutal mace bound with bone.", NamedTextColor.GRAY),
                Component.text("Slams targets after a 4+ block fall.", NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(maceKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(playerKillsKey, PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(upgradedKey, PersistentDataType.BYTE, (byte) 0);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE || !item.hasItemMeta()) return false;
        Byte marked = item.getItemMeta().getPersistentDataContainer().get(maceKey, PersistentDataType.BYTE);
        return marked != null && marked == (byte) 1;
    }

    private ItemStack heldMace(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (isMace(main)) return main;
        ItemStack off = player.getInventory().getItemInOffHand();
        return isMace(off) ? off : null;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!event.getMessage().equalsIgnoreCase("/bonemace")) return;
        if (!event.getPlayer().hasPermission("bonemace.give")) return;
        event.setCancelled(true);
        event.getPlayer().getInventory().addItem(createMace()).values()
                .forEach(item -> event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), item));
        event.getPlayer().sendMessage(Component.text("You received the Bone Handle Mace.", NamedTextColor.GREEN));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item dropped && isMace(dropped.getItemStack())
                && (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Player player
                && event.getCause() == EntityDamageEvent.DamageCause.FALL && heldMace(player) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Long until = stunnedUntil.get(player.getUniqueId());
        if (until != null && until > System.currentTimeMillis() && event.getTo() != null
                && (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ())) {
            event.setTo(event.getFrom().setDirection(event.getTo().getDirection()));
            return;
        }
        if (player.isOnGround()) {
            fallHeights.remove(player.getUniqueId());
        } else if (player.getVelocity().getY() < 0) {
            fallHeights.merge(player.getUniqueId(), player.getLocation().getY(), Math::max);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        fallHeights.remove(event.getPlayer().getUniqueId());
        stunnedUntil.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        ItemStack mace = heldMace(attacker);
        if (mace == null || !(event.getEntity() instanceof LivingEntity target)) return;
        ItemMeta meta = mace.getItemMeta();
        boolean upgraded = meta.getPersistentDataContainer().getOrDefault(upgradedKey, PersistentDataType.BYTE, (byte) 0) == 1;
        double height = Math.max(0.0, fallHeights.getOrDefault(attacker.getUniqueId(), (double) attacker.getFallDistance()));
        event.setDamage(4.0 + (height >= 4 ? height - 3 : 0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0, true, true));
        int kills = meta.getPersistentDataContainer().getOrDefault(playerKillsKey, PersistentDataType.INTEGER, 0);
        if (Math.random() * 100 < (upgraded ? Math.min(100, kills * 10) : 10)) stun(target);
        if (height >= 4) {
            target.setVelocity(new Vector(0, Math.min(1.2, 0.45 + (height - 4) * 0.08), 0));
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 18, 0.45, 0.35, 0.45, 0.1);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f);
        }
        if (upgraded) event.setDamage(event.getDamage() + 5.0);
    }

    private void stun(LivingEntity target) {
        if (target instanceof Player player) {
            long until = System.currentTimeMillis() + 5000;
            stunnedUntil.put(player.getUniqueId(), until);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 128, true, false));
            player.sendMessage(Component.text("You have been stunned!", NamedTextColor.RED));
            Bukkit.getScheduler().runTaskLater(this, () -> stunnedUntil.remove(player.getUniqueId(), until), 101L);
        } else {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10, true, false));
        }
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        ItemStack mace = heldMace(killer);
        if (mace == null) return;
        boolean playerKill = event.getEntity() instanceof Player;
        String type = event.getEntity().getType().name();
        boolean boss = type.equals("WARDEN") || type.equals("WITHER") || type.equals("ENDER_DRAGON");
        if (Math.random() * 100 >= (playerKill || boss ? 20 : 5)) return;
        ItemMeta meta = mace.getItemMeta();
        meta.getPersistentDataContainer().set(upgradedKey, PersistentDataType.BYTE, (byte) 1);
        if (playerKill) {
            int kills = meta.getPersistentDataContainer().getOrDefault(playerKillsKey, PersistentDataType.INTEGER, 0);
            meta.getPersistentDataContainer().set(playerKillsKey, PersistentDataType.INTEGER, Math.min(10, kills + 1));
        }
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                new AttributeModifier(new NamespacedKey(this, "bone_mace_bonus_damage"), 5.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        int stunChance = Math.min(100, meta.getPersistentDataContainer()
                .getOrDefault(playerKillsKey, PersistentDataType.INTEGER, 0) * 10);
        meta.lore(List.of(Component.text("A brutal mace bound with bone.", NamedTextColor.GRAY),
                Component.text("Upgraded: +5 attack damage", NamedTextColor.GOLD),
                Component.text("Stun chance: " + stunChance + "%", NamedTextColor.RED),
                Component.text("Wind charge: 1 | Blast resistant", NamedTextColor.GRAY)));
        mace.setItemMeta(meta);
        killer.sendMessage(Component.text("Your Bone Mace has awakened!", NamedTextColor.GOLD));
    }
}
