package name.soy.asgui;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import me.clip.placeholderapi.PlaceholderAPI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class AsGUI extends JavaPlugin implements Listener {
	Map<String, GUI> guiList = new HashMap<>();
	Map<Player, UserGUI> openGui = new HashMap<>();
	String defaultguiname;
	Material defaultItem;
	private static AsGUI instance;
	boolean hasPAPI = false;
	public void onEnable() {
		instance = this;
		
		saveDefaultConfig();
		reloadConfig();
		for (String key : getConfig().getConfigurationSection("gui").getKeys(false)) {
			ConfigurationSection section = getConfig().getConfigurationSection("gui." + key);
			this.guiList.put(key, GUI.create(section));
		}
		defaultguiname = getConfig().getString("default-gui");
		defaultItem = Material.getMaterial(getConfig().getString("default-item").toUpperCase());
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		getServer().getPluginManager().registerEvents(this, this);
		super.onEnable();
	}
	//传送至服务器的方法
	public static void ToServer(Player player, String server) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(server);
		player.sendPluginMessage(instance, "BungeeCord", out.toByteArray());
	}

	public void onDisable() {
		this.guiList.clear();

		for (Player p : Bukkit.getOnlinePlayers()) {
			if (this.openGui.containsKey(p)) {
				((UserGUI) this.openGui.remove(p)).close();
			}
		}
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length >= 1 && sender instanceof Player) {
			Player p = (Player) sender;
			if (args[0].equals("open")) {
				if (args.length >= 2 && this.guiList.containsKey(args[1]) && !this.openGui.containsKey(p)) {
					boolean flag;//表示能否打开
					GUI gui = this.guiList.get(args[1]);
					if (gui.permission == null) {
						flag = true;
					} else if (gui.permission.startsWith("!")) {
						flag = !p.hasPermission(gui.permission.substring(1));
					} else {
						flag = p.hasPermission(gui.permission);
					}
					if (flag) {
						this.openGui.put(p, gui.showToPlayer(p));
					}
				} else {
					if(this.guiList.containsKey(defaultguiname) && !this.openGui.containsKey(p)) {
						boolean flag;//表示能否打开
						GUI gui = this.guiList.get(defaultguiname);
						if (gui.permission == null) {
							flag = true;
						} else if (gui.permission.startsWith("!")) {
							flag = !p.hasPermission(gui.permission.substring(1));
						} else {
							flag = p.hasPermission(gui.permission);
						}
						if (flag) {
							this.openGui.put(p, gui.showToPlayer(p));
						}
					}
				}
			} else if (args[0].equals("close") && this.openGui.containsKey(p)) {
				((UserGUI) this.openGui.remove(p)).close();
			}else if (args[0].equals("reload") && sender.hasPermission("asgui.reload")) {
				getServer().getPluginManager().disablePlugin(this);
				getServer().getPluginManager().enablePlugin(this);
				sender.sendMessage("[AsGUI] AsGUI reload done");
			}else if(args[0].equals("clear") && sender.hasPermission("asgui.clear")) {
				for(World w:getServer().getWorlds()) {
					for(Entity e:w.getEntities()) {
						if(e.getScoreboardTags().contains("as-gui-item")) {
							e.remove();
						}
					}
				}
			}else if(args[0].equals("help")) {
				sender.sendMessage("§e=======§b全息菜单命令指南§e=======");
				sender.sendMessage("§6/gui open <name> §1打开菜单,name缺省打开默认菜单");
				sender.sendMessage("§6/gui close §1关闭当前菜单");
				if(sender.hasPermission("asgui.reload"))
					sender.sendMessage("§6/gui reload §1重新载入菜单配置");
				
				if(sender.hasPermission("asgui.clear")) {
					sender.sendMessage("§6/gui clear §1清除菜单");
					sender.sendMessage("§c<警告>在没必要时不要使用,作用是如遇到服务器崩溃的时候，清除菜单物品使用");
				}
				
			}
			
		}
		
		return super.onCommand(sender, command, label, args);
	}
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		String[] args0 = new String[] {"open","close","help","reload","clear"};
		//System.out.println(Arrays.asList(args));
		if(args.length==1) {
			List<String> ss= Arrays.asList(args0);
			List<String> s2 = new ArrayList<>();
			ss.stream().filter(args[0]::startsWith).forEach(s2::add);
			return s2;
		}else if(args[0].equals("open")&&args.length>=2) {
			Set<String> o = guiList.keySet();
			List<String> s2 = new ArrayList<>();
			o.stream().filter(args[1]::startsWith).forEach(s2::add);
			return s2;
		}
		return new ArrayList<>();
	}
	//获取插件实例
	public static AsGUI getPlugin() {
		return instance;
	}
	//移动之后，就要关闭GUI了
	@EventHandler(ignoreCancelled = true)
	public void move(PlayerMoveEvent e) { 
		if (this.openGui.containsKey(e.getPlayer())) {
			UserGUI gui = (UserGUI) this.openGui.get(e.getPlayer());
			if (!gui.openLocation.getWorld().equals(e.getTo().getWorld())
					|| gui.openLocation.distance(e.getTo()) > 0.1D) {
				if(!gui.what.moveable)
					((UserGUI) this.openGui.remove(e.getPlayer())).close();
				else {
					gui.openLocation.setX(e.getTo().getX());
					gui.openLocation.setY(e.getTo().getY());
					gui.openLocation.setZ(e.getTo().getZ());
				}
			}
			gui.focus(e.getTo().getYaw());
		}
	}
	public String PAPIed(Player who,String text) {
		text = text.replace('&', '§').replace("§§","&");
		if(hasPAPI)
			return PlaceholderAPI.setPlaceholders(who, text);
		else return text;
	}
	@EventHandler(ignoreCancelled = true)
	public void teleport(PlayerTeleportEvent e) {
		if (this.openGui.containsKey(e.getPlayer())) {
			this.openGui.remove(e.getPlayer()).close();
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void death(PlayerDeathEvent e) {
		if (this.openGui.containsKey(e.getEntity())) {

			this.openGui.remove(e.getEntity()).close();
		}
	}

	@EventHandler
	public void scroll(PlayerItemHeldEvent e) {
		if (this.openGui.containsKey(e.getPlayer())) {
			UserGUI gui = (UserGUI) this.openGui.get(e.getPlayer());
			if (e.getPreviousSlot() - e.getNewSlot() == 1 || (e.getPreviousSlot() == 0 && e.getNewSlot() == 8)) {
				gui.previus--;
			}
			if (e.getPreviousSlot() - e.getNewSlot() == -1 || (e.getPreviousSlot() == 8 && e.getNewSlot() == 0)) {
				gui.previus++;
			}
		}
	}

	@EventHandler
	public void quit(PlayerQuitEvent e) {
		if (this.openGui.containsKey(e.getPlayer())) {
			this.openGui.remove(e.getPlayer()).close();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void interact(PlayerInteractEvent e) {
		if(e.getAction().equals(Action.RIGHT_CLICK_AIR)||e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
			if (this.openGui.containsKey(e.getPlayer())) {
				e.setCancelled(true);
				UserGUI gui = this.openGui.get(e.getPlayer());
				if (gui.foc != null)
					gui.foc.execute();
			} else {
				ItemStack is = e.getPlayer().getInventory().getItemInMainHand();
				if(is.getType().equals(defaultItem)&&!defaultItem.equals(Material.AIR)) {
					this.openGui.put(e.getPlayer(), this.guiList.get(defaultguiname).showToPlayer(e.getPlayer()));
					e.setCancelled(true);
				}
			}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void interact(PlayerInteractAtEntityEvent e) {
		for (UserGUI gui : this.openGui.values()) {
			for (UserGUIEntry<?> entry : gui.openEntries) {
				AsUserGUIEntry e1 = (AsUserGUIEntry) entry;
				if (e.getRightClicked().equals(e1.as) || e.getRightClicked().equals(e1.item)) {
					e.setCancelled(true);
				}
			}
		}
		if (this.openGui.containsKey(e.getPlayer())) {
			UserGUI gui = (UserGUI) this.openGui.get(e.getPlayer());
			gui.foc.execute();
		}
	}

//	@EventHandler
//	public void as(PlayerArmorStandManipulateEvent e) {
//		for (UserGUI gui : this.openGui.values()) {
//			for (UserGUIEntry<?> entry : gui.openEntries) {
//				AsUserGUIEntry e1 = (AsUserGUIEntry) entry;
//				if (e.getRightClicked().equals(e1.as) || e.getRightClicked().equals(e1.item))
//					e.setCancelled(true);
//			}
//		}
//	}
}
