package org.fatecrafters.plugins.commands;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.fatecrafters.plugins.MysqlFunctions;
import org.fatecrafters.plugins.RealisticBackpacks;

public class MainCommand implements CommandExecutor {

	private final RealisticBackpacks plugin;

	private boolean exist = false;

	public MainCommand(final RealisticBackpacks plugin) {
		this.plugin = plugin;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
		if (cmd.getName().equalsIgnoreCase("rb")) {
			if (args.length >= 1) {
				if (args[0].equalsIgnoreCase("reload")) {
					if (!sender.hasPermission("rb.reload")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("noPermission")));
						return false;
					}
					final Long first = System.currentTimeMillis();
					plugin.reloadConfig();
					plugin.setupLists();
					plugin.getServer().resetRecipes();
					plugin.setup();
					sender.sendMessage(ChatColor.GRAY + "Config reloaded.");
					sender.sendMessage(ChatColor.GRAY + "Took " + ChatColor.YELLOW + (System.currentTimeMillis() - first) + "ms" + ChatColor.GRAY + ".");
					return true;
				} else if (args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("purchase")) {
					if (!plugin.isUsingVault()) {
						sender.sendMessage(ChatColor.RED + "This command is disabled due to Vault not being installed.");
						return false;
					}
					if (!(sender instanceof Player)) {
						sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
						return false;
					}
					if (!(args.length == 2)) {
						sender.sendMessage(ChatColor.RED + "Incorrect syntax. Please use:" + ChatColor.GRAY + " /rb buy <backpack>");
						return false;
					}
					String backpack = args[1];
					for (final String b : plugin.backpacks) {
						if (b.equalsIgnoreCase(backpack)) {
							backpack = b;
						}
					}
					if (!plugin.backpacks.contains(backpack)) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("backpackDoesNotExist")));
						return false;
					}
					if (!sender.hasPermission("rb." + backpack + ".buy")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("noPermission")));
						return false;
					}
					if (!plugin.backpackData.get(backpack).get(13).equals("true")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("notPurchasable")));
						return false;
					}
					final double price = Double.parseDouble(plugin.backpackData.get(backpack).get(14));
					if (RealisticBackpacks.econ.getBalance(sender.getName()) < price) {
						sender.sendMessage(ChatColor.RED + "You can not afford " + ChatColor.GOLD + price + ChatColor.RED + " to purchase this backpack.");
						return false;
					}
					final Player p = (Player) sender;
					if (p.getInventory().contains(plugin.backpackItems.get(backpack))) {
						sender.sendMessage(ChatColor.RED + "You already have this backpack on you!");
						return false;
					}
					final Inventory inv = p.getInventory();
					if (inv.firstEmpty() != -1) {
						RealisticBackpacks.econ.withdrawPlayer(p.getName(), price);
						inv.addItem(plugin.backpackItems.get(backpack));
						p.updateInventory();
						sender.sendMessage(ChatColor.GREEN + "You have purchased the " + ChatColor.GOLD + backpack + ChatColor.GREEN + " backpack for " + ChatColor.GOLD + price);
						return true;
					} else {
						sender.sendMessage(ChatColor.RED + "Your inventory is full.");
						return false;
					}
				} else if (args[0].equalsIgnoreCase("list")) {
					if (!sender.hasPermission("rb.list")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("noPermission")));
						return false;
					}
					sender.sendMessage(ChatColor.LIGHT_PURPLE + "  Name  " + ChatColor.GOLD + "|" + ChatColor.AQUA + "  Size  " + ChatColor.GOLD + "|" + ChatColor.GREEN + "  Price  ");
					sender.sendMessage(ChatColor.GOLD + "-----------------------------------");
					for (final String backpack : plugin.backpacks) {
						final boolean hasPerm = sender.hasPermission("rb." + backpack + ".buy");
						final List<String> key = plugin.backpackData.get(backpack);
						if (plugin.backpackData.get(backpack).get(13).equalsIgnoreCase("true") && hasPerm) {
							sender.sendMessage(ChatColor.LIGHT_PURPLE + backpack + ChatColor.GOLD + " | " + ChatColor.AQUA + key.get(0) + ChatColor.GOLD + " | " + ChatColor.GREEN + Double.parseDouble(key.get(14)));
						} else if (!plugin.backpackData.get(backpack).get(13).equalsIgnoreCase("true") && hasPerm) {
							sender.sendMessage(ChatColor.LIGHT_PURPLE + backpack + ChatColor.GOLD + " | " + ChatColor.AQUA + key.get(0) + ChatColor.GOLD + " | " + ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("listCommandNotBuyable")));
						} else {
							sender.sendMessage(ChatColor.LIGHT_PURPLE + backpack + ChatColor.GOLD + " | " + ChatColor.AQUA + key.get(0) + ChatColor.GOLD + " | " + ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("listCommandNoPermission")));
						}
					}
				} else if (args[0].equalsIgnoreCase("give")) {
					if (!(args.length == 3)) {
						sender.sendMessage(ChatColor.RED + "Incorrect syntax. Please use:" + ChatColor.GRAY + " /rb give <player> <backpack>");
						return false;
					}
					String backpack = args[2];
					for (final String b : plugin.backpacks) {
						if (b.equalsIgnoreCase(backpack)) {
							backpack = b;
						}
					}
					if (!sender.hasPermission("rb." + backpack + ".give")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("noPermission")));
						return false;
					}
					if (!plugin.backpacks.contains(backpack)) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("backpackDoesNotExist")));
						return false;
					}
					final Player other = plugin.getServer().getPlayer(args[1]);
					if (other == null) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("playerDoesNotExist")));
						return false;
					}
					final Inventory inv = other.getInventory();
					if (inv.firstEmpty() != -1) {
						inv.addItem(plugin.backpackItems.get(backpack));
						other.updateInventory();
						sender.sendMessage(ChatColor.GREEN + "You have given the " + ChatColor.GOLD + backpack + ChatColor.GREEN + " backpack to " + ChatColor.GOLD + other.getName());
						return true;
					} else {
						sender.sendMessage(ChatColor.RED + "Your inventory is full.");
						return false;
					}
				} else if (args[0].equalsIgnoreCase("filetomysql")) {
					if (!sender.hasPermission("rb.filetomysql")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("noPermission")));
						return false;
					}
					if (!MysqlFunctions.checkIfTableExists("rb_data")) {
						MysqlFunctions.createTables();
						exist = false;
					} else {
						exist = true;
					}
					plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
						@Override
						public void run() {
							try {
								final Connection conn = DriverManager.getConnection(plugin.getUrl(), plugin.getUser(), plugin.getPass());
								final File dir = new File(plugin.getDataFolder() + File.separator + "userdata");
								int i = 0, times = 0;
								final int files = dir.listFiles().length;
								for (final File child : dir.listFiles()) {
									final FileConfiguration config = YamlConfiguration.loadConfiguration(child);
									final String player = child.getName().replace(".yml", "");
									i++;
									final Statement statement = conn.createStatement();
									PreparedStatement state = null;
									for (final String backpack : config.getConfigurationSection("").getKeys(false)) {
										if (exist) {
											final ResultSet res = statement.executeQuery("SELECT EXISTS(SELECT 1 FROM rb_data WHERE player = '" + player + "' AND backpack = '" + backpack + "' LIMIT 1);");
											if (res.next()) {
												if (res.getInt(1) == 1) {
													state = conn.prepareStatement("UPDATE rb_data SET player='" + player + "', backpack='" + backpack + "', inventory='" + config.getString(backpack + ".Inventory") + "' WHERE player='" + player + "' AND backpack='" + backpack + "';");
												} else {
													state = conn.prepareStatement("INSERT INTO rb_data (player, backpack, inventory) VALUES('" + player + "', '" + backpack + "', '" + config.getString(backpack + ".Inventory") + "' );");
												}
											}
										} else {
											state = conn.prepareStatement("INSERT INTO rb_data (player, backpack, inventory) VALUES('" + player + "', '" + backpack + "', '" + config.getString(backpack + ".Inventory") + "' );");
										}
										state.executeUpdate();
										state.close();
									}
									if (i == 100) {
										i = 0;
										times++;
										sender.sendMessage(ChatColor.LIGHT_PURPLE + "" + times * 100 + "/" + files + " files have been transferred.");
									}
								}
								conn.close();
								sender.sendMessage(ChatColor.LIGHT_PURPLE + "File transfer complete.");
							} catch (final SQLException e) {
								e.printStackTrace();
							}
						}
					});
				}
			}
		}
		return false;
	}

}