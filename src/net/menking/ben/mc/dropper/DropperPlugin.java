package net.menking.ben.mc.dropper;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Dropper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class DropperPlugin extends JavaPlugin {

	private List<Location> dropperContainer;
	private List<Integer> itemDrops;
	private List<Integer> restrictedItems;
	private List<String> restrictedWorlds;
	private int frequency;
	private UpdaterTask task;
	private boolean debug = false;
	
	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		
		loadConfig();
	}
	
	public void onDisable() {
		if( dropperContainer != null ) {
			dropperContainer.clear();
		}
		
		if( itemDrops != null ) {
			itemDrops.clear();
		}
		
		getServer().getScheduler().cancelTasks(this);
	}
	
	private void loadConfig() {
		
		debug = getConfig().getBoolean("debug", false);
		
		ConsoleCommandSender console = getServer().getConsoleSender();
		
		console.sendMessage("[DROPPER] Reloading configuration");
		
		// clear out or initialize our variables
		//
		if( itemDrops == null ) {
			itemDrops = new ArrayList<Integer>();
		}
		else {
			itemDrops.clear();
		}
		
		if( dropperContainer == null ) {
			dropperContainer = new ArrayList<Location>();
		}
		else {
			dropperContainer.clear();
		}
		
		if( restrictedItems == null ) {
			restrictedItems = new ArrayList<Integer>();
		}
		else {
			restrictedItems.clear();
		}
		
		if( restrictedWorlds == null ) {
			restrictedWorlds = new ArrayList<String>();
		}
		else {
			restrictedWorlds.clear();
		}	
		
		// populate our restricted items list
		//
		String[] restricted = this.getConfig().getString("restricted-items", "").split(",");
		
		for( String item : restricted ) {
			try {
				restrictedItems.add(Integer.parseInt(item));
			} catch(NumberFormatException e) {}
		}
				
		// populate our restricted worlds list
		//
		String[] worlds = this.getConfig().getString("restricted-worlds", "").split(",");
		
		for( String item : worlds ) {
			restrictedWorlds.add(item);
		}
		
		frequency = this.getConfig().getInt("drop-frequency");
		
		String[] drops = this.getConfig().getString("defaults.dropitems", "").split(",");
	
		for(String drop : drops ) {
			try {
				itemDrops.add(Integer.parseInt(drop));
			} catch( NumberFormatException e) {}
		}
		
		if( task == null ) {
			task = new UpdaterTask(this, debug);
		}
		
		// load in any persistent droppers
		ArrayList<String> droppers = (ArrayList<String>) getConfig().getStringList("droppers");
		
		if( droppers != null ) {
			console.sendMessage("[DROPPER] Found " + Integer.toString(droppers.size()) + " droppers to add");
		
			for( String srcLoc : droppers ) {
				String[] data = srcLoc.split(",");
				
				Location loc = new Location(getServer().getWorld(data[0]), Double.parseDouble(data[1]), 
						Double.parseDouble(data[2]), Double.parseDouble(data[3]));
				
				if( loc != null ) {
					console.sendMessage("[DROPPER] adding dropper @ " + srcLoc);
					dropperContainer.add(loc);
				}
			}
		}
		else {
			console.sendMessage("[DROPPER] No droppers to register");
		}
		
		getServer().getScheduler().scheduleSyncRepeatingTask(this,  task,  20, frequency * 20);
		
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args ) {
		if( !(sender instanceof Player) ) {
			sender.sendMessage("Sorry, console cannot use this plugin");
			return true;
		}
		
		Player me = (Player) sender;
		
		if( (cmd.getName().equalsIgnoreCase("contribute") || cmd.getName().equalsIgnoreCase("contrib") ) && me.hasPermission("donation.give")) {
			PlayerInventory pi = me.getInventory();
			
			ItemStack items = pi.getItemInHand();
			
			// make sure the player's world is allowable
			//
			for( String w : restrictedWorlds ) {
				if( w.equalsIgnoreCase(me.getWorld().getName())) {
					me.sendMessage(ChatColor.YELLOW + "Sorry, you can't donate in this world" + ChatColor.RESET);
					return true;					
				}
			}
			
			// make sure the item(s) are not restricted
			//
			for(Integer i : restrictedItems ) {
				if( items.getTypeId() == i ) {
					me.sendMessage(ChatColor.YELLOW + "Sorry, you can't donate that" + ChatColor.RESET);
					return true;
				}
			}
			
			pi.clear(pi.getHeldItemSlot());
			
			me.sendMessage(ChatColor.GREEN + "You just donated " + Integer.toString(items.getAmount())
					+ " " + items.getData().getItemType().name() + ".  The destitute thank you!" + ChatColor.RESET);
			
			if( !dropperContainer.isEmpty() ) {
				for(Location loc : dropperContainer ) {
					
					Block realBlock = me.getWorld().getBlockAt(loc);
					
					if( realBlock.getState() instanceof Dropper ) {
						Dropper d = (Dropper) realBlock.getState();

						d.getInventory().addItem(items);
						
						break;
					}
					else {
						getServer().getConsoleSender().sendMessage("[DROPPER] ERROR: That dropper is not an instanceof a dropper! ");
					}
				}
			}
			else { 
				getServer().getConsoleSender().sendMessage("[DROPPER] WARNING: " + Integer.toString(items.getAmount())
						+ " " + items.getData().getItemType().name() + " were donated, but no dropper were enabled");
			}
		}
		else if( cmd.getName().equalsIgnoreCase("dropper") ) {
			if( args.length > 0 ) {
				if( args[0].equalsIgnoreCase("add") && me.hasPermission("donation.admin.add")) {
					Block block = this.getSelectedBlock(me);
					
					if(debug) me.sendMessage("Block: " + block.toString());
					
					if( block.getState() instanceof Dropper ) {
						// see if we already have this block in the container
						if( !dropperContainer.contains(block.getLocation())) { 
							dropperContainer.add(block.getLocation());
							me.sendMessage(ChatColor.GREEN + "Registered dropper at " + block.toString() + ChatColor.RESET);
							
							ArrayList<String> droppers = (ArrayList<String>) getConfig().getStringList("droppers");
							
							String locStr = block.getLocation().getWorld().getName() + ","
									+ Double.toString(block.getLocation().getX()) + ","
									+ Double.toString(block.getLocation().getY()) + ","
									+ Double.toString(block.getLocation().getZ());
							
							droppers.add(locStr);
							
							getConfig().set("droppers", droppers);
							
							saveConfig();
						}
						else {
							sender.sendMessage(ChatColor.YELLOW + "That dropper is already registered" + ChatColor.RESET);
						}
						return true;
					}
					else {
						sender.sendMessage(ChatColor.RED + "That's not a dropper!" + ChatColor.RESET);
						return true;
					}
				}
				else if( args[0].equalsIgnoreCase("remove" ) && me.hasPermission("donation.admin.remove")) {
					Block block = this.getSelectedBlock(me);
					
					if( dropperContainer.contains(block.getLocation()) ) {
						dropperContainer.remove(block.getLocation());
						sender.sendMessage(ChatColor.GREEN + "That dropper has been removed" + ChatColor.RESET);
					}
					else {
						sender.sendMessage(ChatColor.RED + "Sorry, that dispenser is not in the database" + ChatColor.RESET);
					}
					return true;
				}
				else if( args[0].equalsIgnoreCase("reload" ) && me.hasPermission("donation.admin.reload")) {
					getServer().getScheduler().cancelTasks(this);
					loadConfig();
				}
			}
		}
		
		return false;
	}
	
	private Block getSelectedBlock(Player p) {
		Block b = p.getTargetBlock(null,  200);
		return b;
	}
	
	public synchronized Location[] getDroppers() {
		// why do we do this?  we don't want the async thread to have any
		// references to our dropperContainer in this thread
		//
		return (Location[]) this.dropperContainer.toArray(new Location[dropperContainer.size()]);
	}
	
}
