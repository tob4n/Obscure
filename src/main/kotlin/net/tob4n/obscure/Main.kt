package net.tob4n.obscure

import com.comphenix.protocol.ProtocolManager
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin(), Listener {

    private lateinit var protocolManager: ProtocolManager

    override fun onEnable() {
        NameManager(Main(), protocolManager)
        server.pluginManager.registerEvents(this, this)
    }

    override fun onDisable() {
        protocolManager.removePacketListeners(this)
    }

}