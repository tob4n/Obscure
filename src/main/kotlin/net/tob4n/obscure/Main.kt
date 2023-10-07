package net.tob4n.obscure

import com.comphenix.protocol.ProtocolLibrary
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {

    override fun onEnable() {
        NameManager(this)
    }

    override fun onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this)
    }
}