package net.menking.ben.mc.dropper;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Dropper;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class UpdaterTask extends BukkitRunnable {

	private final JavaPlugin plugin;
	private boolean debug = false;
	
	public UpdaterTask(JavaPlugin plugin, boolean debug) {
		this.plugin = plugin;
		this.debug = debug;
	}
	
	@Override
	public void run() {
		Location[] locations = ((DropperPlugin)plugin).getDroppers();
		
		if( debug )
			plugin.getServer().getConsoleSender().sendMessage("[DROPPER] Updater Task starting");
		
		for( Location loc : locations ) { 
			Block b = loc.getWorld().getBlockAt(loc);
			
			Dropper d = (Dropper) b.getState();
			
			d.drop();
		}
		
		if( debug ) 
			plugin.getServer().getConsoleSender().sendMessage("[DROPPER] Updater Task finished");
	}	

}
