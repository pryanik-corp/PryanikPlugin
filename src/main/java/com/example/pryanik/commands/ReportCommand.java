package com.example.pryanik.commands;

import com.example.pryanik.PryanikPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.*;

public final class ReportCommand implements CommandExecutor, Listener {
    private static final String TARGET_TITLE = "§8Пожаловаться: игрок";
    private static final String REASON_TITLE = "§8Причина жалобы";
    private static final String CHAT_TITLE = "§8Жалоба: чат";
    private static final String CHEAT_TITLE = "§8Жалоба: читы";
    private static final Set<String> CHAT_REASONS = Set.of("Оскорбление", "Мат", "Спам", "Реклама", "Флуд", "Другое (чат)");
    private static final Set<String> CHEAT_REASONS = Set.of("Xray", "Aim / KillAura", "Fly", "Speed", "AutoClicker", "Другое (читы)");
    private final PryanikPlugin plugin;
    private final Map<UUID, UUID> selected = new HashMap<>();
    private final Map<UUID, Long> openedAt = new HashMap<>();

    public ReportCommand(PryanikPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Только игрок может отправить жалобу."); return true; }
        if (!plugin.authed(player.getUniqueId())) { player.sendMessage("§cСначала войдите."); return true; }
        openTargets(player);
        return true;
    }

    private void openTargets(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TARGET_TITLE);
        Set<UUID> added = new HashSet<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(player.getUniqueId())) { addHead(inv, online.getUniqueId(), online.getName()); added.add(online.getUniqueId()); }
        }
        try {
            for (String[] row : plugin.db().recentPlayers(10)) {
                UUID id = UUID.fromString(row[0]);
                if (!id.equals(player.getUniqueId()) && added.add(id)) addHead(inv, id, row[1]);
            }
        } catch (Exception ex) { player.sendMessage("§cНе удалось загрузить список игроков."); plugin.getLogger().warning("Report player list: " + ex.getMessage()); }
        player.openInventory(inv);
    }

    private void addHead(Inventory inv, UUID id, String name) {
        if (inv.firstEmpty() < 0) return;
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        OfflinePlayer offline = Bukkit.getOfflinePlayer(id);
        meta.setOwningPlayer(offline);
        meta.setDisplayName("§e" + name);
        meta.setLore(List.of("§7Нажмите, чтобы выбрать игрока"));
        head.setItemMeta(meta);
        inv.addItem(head);
    }

    private void openReasons(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, REASON_TITLE);
        put(inv, 11, Material.IRON_SWORD, "§cЧиты", "§7Xray, aim, fly и другое");
        put(inv, 15, Material.PAPER, "§eЧат", "§7Оскорбления, мат, спам и другое");
        p.openInventory(inv);
    }
    private void openReasonList(Player p, boolean chat) {
        String title = chat ? CHAT_TITLE : CHEAT_TITLE;
        Inventory inv = Bukkit.createInventory(null, 27, title);
        String[] reasons = (chat ? CHAT_REASONS : CHEAT_REASONS).toArray(new String[0]);
        Arrays.sort(reasons);
        for (int i=0; i<reasons.length; i++) put(inv, 10+i, chat ? Material.PAPER : Material.IRON_SWORD, "§e"+reasons[i], "§7Нажмите, чтобы отправить жалобу");
        p.openInventory(inv);
    }
    private void put(Inventory inv, int slot, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material); ItemMeta meta=item.getItemMeta(); meta.setDisplayName(name); meta.setLore(Arrays.asList(lore)); item.setItemMeta(meta); inv.setItem(slot,item);
    }

    @EventHandler public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title=event.getView().getTitle();
        if (!title.equals(TARGET_TITLE)&&!title.equals(REASON_TITLE)&&!title.equals(CHAT_TITLE)&&!title.equals(CHEAT_TITLE)) return;
        event.setCancelled(true);
        if (!plugin.authed(player.getUniqueId()) || event.getRawSlot()<0 || event.getRawSlot()>=event.getView().getTopInventory().getSize()) return;
        ItemStack item=event.getCurrentItem(); if(item==null||!item.hasItemMeta()) return;
        if(title.equals(TARGET_TITLE)) {
            if(item.getType()!=Material.PLAYER_HEAD) return;
            SkullMeta meta=(SkullMeta)item.getItemMeta(); OfflinePlayer owner=meta.getOwningPlayer();
            if(owner==null||owner.getUniqueId().equals(player.getUniqueId()))return;
            selected.put(player.getUniqueId(),owner.getUniqueId()); openedAt.put(player.getUniqueId(),System.currentTimeMillis()); openReasons(player); return;
        }
        if(title.equals(REASON_TITLE)) {
            String name=item.getItemMeta().getDisplayName();
            if(name.contains("Читы")) openReasonList(player,false); else if(name.contains("Чат")) openReasonList(player,true); return;
        }
        String display=item.getItemMeta().getDisplayName(); String reason=display.replaceFirst("^§[0-9a-fk-or]", "");
        if(!(title.equals(CHAT_TITLE)?CHAT_REASONS:CHEAT_REASONS).contains(reason)) return;
        UUID target=selected.remove(player.getUniqueId()); Long when=openedAt.remove(player.getUniqueId());
        if(target==null||when==null||System.currentTimeMillis()-when>120_000L){player.closeInventory();player.sendMessage("§cСессия жалобы истекла. Используйте /report заново.");return;}
        player.closeInventory();
        try {
            if(plugin.db().hasReported(target,player.getUniqueId())) { player.sendMessage("§cВы уже отправляли жалобу на этого игрока."); return; }
            long count=plugin.db().saveReport(target,player.getUniqueId(),reason);
            player.sendMessage("§aЖалоба отправлена. Спасибо!");
            Player accused=Bukkit.getPlayer(target);
            if(count>=3&&accused!=null) { accused.kickPlayer("§cВы были отключены: на вас поступило 3 жалобы от разных игроков."); Bukkit.broadcastMessage("§cИгрок §f"+accused.getName()+" §cотключён после 3 жалоб от разных игроков."); }
        } catch(Exception ex) { player.sendMessage("§cНе удалось сохранить жалобу."); plugin.getLogger().warning("Report save failed: "+ex.getMessage()); }
    }
}
