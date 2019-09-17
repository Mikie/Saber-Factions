package com.massivecraft.factions.shop;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.util.XMaterial;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopGUIFrame {

    private Gui gui;

    public ShopGUIFrame(Faction f) {
        gui = new Gui(FactionsPlugin.getInstance(),
                FactionsPlugin.getInstance().getConfig().getInt("F-Shop.GUI.Rows", 4),
                FactionsPlugin.getInstance().color(FactionsPlugin.getInstance().getConfig().getString("F-Shop.GUI.Name")));
    }

    public void buildGUI(FPlayer fplayer) {
        PaginatedPane pane = new PaginatedPane(0, 0, 9, gui.getRows());
        List<GuiItem> GUIItems = new ArrayList<>();
        ItemStack dummy = buildDummyItem(fplayer.getFaction());
        for (int x = 0; x <= (gui.getRows() * 9) - 1; x++) GUIItems.add(new GuiItem(dummy, e -> e.setCancelled(true)));

        int items = ShopConfig.getShop().getConfigurationSection("items").getKeys(false).size();
        for (int shopitems = 1; shopitems <= items; shopitems++) {
            String s = shopitems + "";
            String name = ShopConfig.getShop().getString("items." + s + ".name");
            int slot = ShopConfig.getShop().getInt("items." + s + ".slot");

            GUIItems.set(slot, new GuiItem(buildShopAssets(fplayer.getFaction()), e -> {
                e.setCancelled(true);

                FPlayer fme = FPlayers.getInstance().getByPlayer((Player) e.getWhoClicked());
                int cost = ShopConfig.getShop().getInt("items." + s + ".cost");
                if (fplayer.getFaction().getPoints() >= cost) {
                    fplayer.getFaction().setPoints(fplayer.getFaction().getPoints() - cost);
                    runCommands(ShopConfig.getShop().getStringList("items." + s + ".cmds"), fplayer.getPlayer());
                    for (FPlayer fplayerBuy : fplayer.getFaction().getFPlayers()) {
                        fplayerBuy.getPlayer().sendMessage(TL.SHOP_BOUGHT_BROADCAST_FACTION.toString()
                                .replace("{player}", fplayer.getPlayer().getName())
                                .replace("{item}", ChatColor.stripColor(FactionsPlugin.getInstance().color(name))).replace("{cost}", cost + ""));
                    }
                    buildGUI(fme);
                } else {
                    fplayer.msg(TL.SHOP_NOT_ENOUGH_POINTS);
                }
            }));
            pane.populateWithGuiItems(GUIItems);
            gui.addPane(pane);
            gui.update();
            gui.show(fplayer.getPlayer());
        }
    }

    private ItemStack buildShopAssets(Faction f) {
        int items = ShopConfig.getShop().getConfigurationSection("items").getKeys(false).size();
        ItemStack item = null;
        for (int shopitems = 1; shopitems <= items; shopitems++) {
            String s = shopitems + "";
            ConfigurationSection config = ShopConfig.getShop().getConfigurationSection("items." + s);
            boolean glowing = config.getBoolean("glowing");
            item = XMaterial.matchXMaterial(config.getString("block")).parseItem();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setLore(FactionsPlugin.getInstance().colorList(config.getStringList("lore")));
                meta.setDisplayName(FactionsPlugin.getInstance().color(config.getString("name")));
                if (glowing) {
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    meta.addEnchant(Enchantment.DURABILITY, 1, true);
                }
                if (!glowing) {
                    meta.removeEnchant(Enchantment.DURABILITY);
                }
                item.setItemMeta(meta);
            }
        }
        return item;
    }


    private ItemStack buildDummyItem(Faction f) {
        ConfigurationSection config = FactionsPlugin.getInstance().getConfig().getConfigurationSection("F-Shop.GUI.dummy-item");
        ItemStack item = XMaterial.matchXMaterial(config.getString("Type")).parseItem();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(FactionsPlugin.getInstance().colorList(config.getStringList("Lore")));
            meta.setDisplayName(FactionsPlugin.getInstance().color(config.getString("Name").replace("{points}", f.getPoints() + "")));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void runCommands(List<String> list, Player p) {
        for (String cmd : list) {
            cmd = cmd.replace("%player%", p.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }
}