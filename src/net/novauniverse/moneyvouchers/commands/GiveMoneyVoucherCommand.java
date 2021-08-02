package net.novauniverse.moneyvouchers.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import net.novauniverse.moneyvouchers.NovaMoneyVouchers;
import net.zeeraa.novacore.spigot.command.AllowedSenders;
import net.zeeraa.novacore.spigot.command.NovaCommand;

public class GiveMoneyVoucherCommand extends NovaCommand {

	public GiveMoneyVoucherCommand() {
		super("givemoneyvoucher", NovaMoneyVouchers.getInstance());

		setFilterAutocomplete(true);
		setPermission("novamoneyvouchers.givemoneyvoucher");
		setPermissionDefaultValue(PermissionDefault.OP);
		setAllowedSenders(AllowedSenders.ALL);
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		if (!NovaMoneyVouchers.getInstance().isEnabled()) {
			sender.sendMessage(ChatColor.RED + "This plugin is not enabled right now");
			return false;
		}

		if (args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Please provide a player");
			return false;
		}

		Player player = Bukkit.getServer().getPlayer(args[0]);

		if (player == null) {
			sender.sendMessage(ChatColor.RED + "Could not find a player named " + args[0]);
			return false;
		}

		if (!player.isOnline()) {
			sender.sendMessage(ChatColor.RED + "That player is not online");
			return false;
		}

		if (args.length == 1) {
			sender.sendMessage(ChatColor.RED + "Please provide the value of the voucher");
			return false;
		}

		try {
			double value = Double.parseDouble(args[1]);

			player.getInventory().addItem(NovaMoneyVouchers.getInstance().getMoneyVoucher(value));
		} catch (NumberFormatException e) {
			sender.sendMessage(ChatColor.RED + "Please provide a valid value for the voucher");
			return false;
		}

		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
		List<String> result = new ArrayList<>();

		if (args.length == 0 || args.length == 1) {
			for (Player player : Bukkit.getServer().getOnlinePlayers()) {
				if (sender instanceof Player) {
					if (!((Player) sender).canSee(player)) {
						continue;
					}
				}

				result.add(player.getName());
			}
		}

		return result;
	}
}