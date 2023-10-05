package net.tob4n.obscure

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolManager
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.WrappedDataWatcher
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerMoveEvent

class NameManager(private val plugin: Main, private val protocolManager: ProtocolManager) {

    private val loadedPlayers = mutableMapOf<Player, MutableSet<Player>>()
    private val hiddenPlayers = mutableMapOf<Player, MutableSet<Player>>()
    private val playerIds: Map<Int, Player> = HashMap()

    init {

        protocolManager.addPacketListener(
            object : PacketAdapter(plugin, PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
                override fun onPacketSending(event: PacketEvent) {
                    val entity = event.packet.getEntityModifier(event).read(0)
                    if (entity is Player) {
                        loadedPlayers.computeIfAbsent(event.player) { mutableSetOf() }.add(entity)
                    }
                }
            }
        )

        protocolManager.addPacketListener(
            object : PacketAdapter(plugin, PacketType.Play.Server.ENTITY_DESTROY) {
                override fun onPacketSending(event: PacketEvent) {
                    val entityIDs = event.packet.integerArrays.read(0)
                    for (id in entityIDs) {
                        val player = playerIds[id]
                        if (player != null) {
                            loadedPlayers[event.player]?.remove(player)
                            hiddenPlayers[event.player]?.remove(player)
                        }
                    }
                }
            }
        )
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val eventPlayer = event.player
        loadedPlayers[eventPlayer]?.forEach { player ->
            if (player.isObscuredFrom(eventPlayer)) {
                if (hiddenPlayers[eventPlayer]?.add(player) == true) {
                    eventPlayer.updateNameFor(player, true)
                }
            } else {
                if (hiddenPlayers[eventPlayer]?.remove(player) == true) {
                    eventPlayer.updateNameFor(player, false)
                }
            }
        }
    }

    private fun Player.updateNameFor(player: Player, hidden: Boolean) {
        val packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA)
        packet.integers.write(0, this.entityId)
        packet.watchableCollectionModifier.write(
            0,
            WrappedDataWatcher()
                .apply {
                    setObject(
                        WrappedDataWatcher.WrappedDataWatcherObject(
                            3,
                            WrappedDataWatcher.Registry.get(Boolean::class.java)
                        ),
                        hidden
                    )
                }
                .watchableObjects
        )
        protocolManager.sendServerPacket(player, packet)
    }

    private fun Player.isObscuredFrom(player: Player): Boolean {
        val blockAbove = this.location.clone().add(0.0, 1.0, 0.0).block
        return if (blockAbove.type.isOccluding) {
            true
        } else {
            val center = blockAbove.location.add(0.5, 0.5, 0.5)
            val result =
                player.world.rayTraceBlocks(
                    center,
                    player.eyeLocation.toVector().subtract(center.toVector()).normalize(),
                    center.distance(player.eyeLocation)
                )
            result?.hitBlock?.type?.isOccluding == true
        }
    }
}