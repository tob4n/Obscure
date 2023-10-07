package net.tob4n.obscure

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.utility.MinecraftReflection
import java.util.*
import net.tob4n.obscure.Algorithm.Companion.bresenham3D
import org.bukkit.ChatColor
import org.bukkit.entity.Player

class NameManager(private val plugin: Main) {

    private val protocolManager = ProtocolLibrary.getProtocolManager()
    private val loadedPlayerCache = mutableMapOf<UUID, HashSet<Player>>()
    private val visiblePlayerCache = mutableMapOf<Pair<UUID, UUID>, Boolean>()

    init {
        plugin.server.scheduler.scheduleSyncRepeatingTask(
            plugin,
            {
                plugin.server.onlinePlayers.forEach { player ->
                    loadedPlayerCache[player.uniqueId]?.forEach { loadedPlayer ->
                        val key = Pair(loadedPlayer.uniqueId, player.uniqueId)
                        val wasVisible = visiblePlayerCache.getOrDefault(key, true)
                        val isVisible = loadedPlayer.isNameVisibleTo(player)
                        if (isVisible != wasVisible) {
                            loadedPlayer.setNameVisibility(player, isVisible)
                            visiblePlayerCache[key] = isVisible
                        }
                    }
                }
            },
            0L,
            5L
        )

        protocolManager.apply {
            addPacketListener(
                object : PacketAdapter(plugin, PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
                    override fun onPacketSending(event: PacketEvent) {
                        (event.packet.getEntityModifier(event).read(0) as? Player)?.let {
                            loadedPlayerCache.getOrPut(event.player.uniqueId) { HashSet() }.add(it)
                        }
                    }
                }
            )

            addPacketListener(
                object : PacketAdapter(plugin, PacketType.Play.Server.ENTITY_DESTROY) {
                    override fun onPacketSending(event: PacketEvent) {
                        event.packet.intLists.read(0)?.forEach { entityId ->
                            loadedPlayerCache[event.player.uniqueId]?.removeIf {
                                it.entityId == entityId
                            }
                        }
                    }
                }
            )
        }
    }

    private fun Player.setNameVisibility(player: Player, visible: Boolean) {
        val packet =
            PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM).apply {
                strings.write(0, entityId.toString())
                integers.write(0, 0)
                optionalStructures.read(0).get().apply {
                    strings.write(0, if (visible) "always" else "never")
                    getEnumModifier(
                        ChatColor::class.java,
                        MinecraftReflection.getMinecraftClass("EnumChatFormat")
                    )
                        .write(0, ChatColor.WHITE)
                }
                getSpecificModifier(Collection::class.java).write(0, listOf(name))
            }
        protocolManager.sendServerPacket(player, packet)
    }

    private fun Player.isNameVisibleTo(player: Player): Boolean {
        val nameTag = location.clone().add(0.0, 2.2, 0.0)
        if (nameTag.block.type.isOccluding) return false
        for (i in bresenham3D(nameTag, player.eyeLocation)) {
            if (i.block.type.isOccluding) return false
        }
        return true
    }
}