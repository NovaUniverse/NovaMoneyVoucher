package net.novauniverse.moneyvouchers;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.novauniverse.moneyvouchers.commands.GiveMoneyVoucherCommand;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.spigot.abstraction.VersionIndependantUtils;
import net.zeeraa.novacore.spigot.command.CommandRegistry;
import net.zeeraa.novacore.spigot.utils.ItemBuilder;

/**
 * A plugin that adds money vouchers
 * 
 * @author Zeeraa
 */
public class NovaMoneyVouchers extends JavaPlugin implements Listener {
	private static NovaMoneyVouchers instance;
	private Economy economy;

	private boolean useVaultCurrencyName;
	private String currencyNameSingular;
	private String currencyNamePlural;
	private boolean broadcastRedeem;
	private Material voucherMaterial;

	/**
	 * Get the instance of the plugin
	 * 
	 * @return {@link NovaMoneyVouchers} inst6ance
	 */
	public static NovaMoneyVouchers getInstance() {
		return instance;
	}

	/**
	 * Get the economy provider from vault
	 * 
	 * @return The {@link Economy} provider
	 */
	public Economy getEconomy() {
		return economy;
	}

	/**
	 * Create a money voucher item stack
	 * 
	 * @param value The value of the voucher
	 * @return {@link ItemStack} with money voucher or <code>null</code> if an error
	 *         occurred
	 */
	@Nullable
	public ItemStack getMoneyVoucher(double value) {
		if (!this.isEnabled()) {
			return null;
		}

		if (value <= 0) {
			return null;
		}

		ItemBuilder builder = new ItemBuilder(voucherMaterial);

		String valueString = formatValue(value);

		builder.setName(valueString + " voucher");

		builder.addLore(ChatColor.WHITE + "This is a voucher for" + valueString);
		builder.addLore(ChatColor.WHITE + "Right click to redeem it");

		builder.addEnchant(Enchantment.DURABILITY, 1, true);
		builder.addItemFlags(ItemFlag.HIDE_ENCHANTS);

		ItemStack stack = builder.build();

		stack = NBTEditor.set(stack, value, "novamoneyvoucher", "vouchervalue");

		return stack;
	}

	private String formatValue(double value) {
		String valueString = value + "";

		if (useVaultCurrencyName) {
			valueString += (value == 1 ? economy.currencyNameSingular() : economy.currencyNamePlural());
		} else {
			valueString += (value == 1 ? currencyNameSingular : currencyNamePlural);
		}

		return valueString;
	}

	@Override
	public void onEnable() {
		NovaMoneyVouchers.instance = this;

		saveDefaultConfig();

		useVaultCurrencyName = getConfig().getBoolean("use_vault_currency_name");
		currencyNameSingular = getConfig().getString("currency_name_singular");
		currencyNamePlural = getConfig().getString("currency_name_plural");
		broadcastRedeem = getConfig().getBoolean("broadcast_redeem");

		String voucherMaterialString = getConfig().getString("voucher_material");
		try {
			voucherMaterial = Material.valueOf(voucherMaterialString);
		} catch (Exception e) {
			Log.fatal("MoneyVouchers", "Invalid material: " + voucherMaterialString + ". Shutting down the plugin");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			Log.fatal("MoneyVouchers", "No economy provider found. Shutting down");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		economy = rsp.getProvider();

		Log.info("MoneyVouchers", "Found economy provider: " + economy.getName());

		Bukkit.getPluginManager().registerEvents(this, this);

		CommandRegistry.registerCommand(new GiveMoneyVoucherCommand());

		Log.info("MoneyVouchers", "MoneyVouchers has beeen enabled");
	}

	@Override
	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
		HandlerList.unregisterAll((Plugin) this);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getItem() != null) {
			if (NBTEditor.contains(e.getItem(), "novamoneyvoucher", "vouchervalue")) {
				if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
					e.setCancelled(true);

					Player player = e.getPlayer();
					if (VersionIndependantUtils.get().getItemInMainHand(player).getAmount() > 1) {
						VersionIndependantUtils.get().getItemInMainHand(player).setAmount(e.getItem().getAmount() - 1);
					} else {
						if (VersionIndependantUtils.get().getItemInMainHand(player).getAmount() == 1) {
							VersionIndependantUtils.get().setItemInMainHand(player, ItemBuilder.AIR);
						} else {
							boolean removed = false;
							for (int i = 0; i < player.getInventory().getSize(); i++) {
								ItemStack item = player.getInventory().getItem(i);
								if (item != null) {
									if (item.getType() != Material.AIR) {
										if (NBTEditor.contains(item, "novamoneyvoucher", "vouchervalue")) {
											if (item.getAmount() > 1) {
												item.setAmount(item.getAmount() - 1);
												removed = true;
												break;
											} else {
												player.getInventory().setItem(i, null);
												removed = true;
												break;
											}
										}
									}
								}
							}

							if (!removed) {
								return;
							}
						}
					}

					double amount = NBTEditor.getDouble(e.getItem(), "novamoneyvoucher", "vouchervalue");

					if (amount > 0) {
						EconomyResponse response = economy.depositPlayer(player, amount);

						if (response.type == EconomyResponse.ResponseType.SUCCESS) {
							if (broadcastRedeem) {
								Bukkit.getServer().broadcastMessage(ChatColor.GOLD + player.getDisplayName() + ChatColor.GOLD + " redeemed a money voucher for " + ChatColor.AQUA + formatValue(amount));
							} else {
								Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + player.getDisplayName() + " redeemed a money voucher for " + formatValue(amount));
							}
							player.sendMessage(ChatColor.AQUA + formatValue(amount) + ChatColor.GOLD + " has been added to you account");
						} else {
							Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "\n" + player.getName() + "Failed to redeem money voucher.\nVault responded with " + response.type.name() + ".\nThe voucher had a value of " + amount);
							player.sendMessage(ChatColor.RED + "An error occured while redeeming voucher. Please contact an admin");
						}
					} else {
						Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "\n" + player.getName() + "Failed to redeem money voucher.\nThe voucher has a value of 0 or less");
						player.sendMessage(ChatColor.RED + "An error occured while redeeming voucher. Please contact an admin");
					}
				}
			}
		}
	}
}