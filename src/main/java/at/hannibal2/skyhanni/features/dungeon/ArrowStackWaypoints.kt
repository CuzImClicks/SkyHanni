package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.WitheredDragonEvent
import at.hannibal2.skyhanni.features.dungeon.m7.WitheredDragonInfo
import at.hannibal2.skyhanni.features.dungeon.m7.WitheredDragonSpawnedStatus
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/*
[
   {
      "x":18,
      "y":5,
      "z":84,
      "r":0,
      "g":1,
      "b":0,
      "options":{
         "name":"Red-1"
      }
   },
   {
      "x":10,
      "y":6,
      "z":83,
      "r":0,
      "g":1,
      "b":0,
      "options":{
         "name":"Red-2"
      }
   },
   {
      "x":53,
      "y":4,
      "z":90,
      "r":0,
      "g":1,
      "b":0,
      "options":{
         "name":"Orange"
      }
   },
   {
      "x":31,
      "y":5,
      "z":97,
      "r":0,
      "g":1,
      "b":0,
      "options":{
         "name":"Purple-1"
      }
   },
   {
      "x":81,
      "y":5,
      "z":99,
      "r":0,
      "g":1,
      "b":0,
      "options":{
         "name":"Purple-2"
      }
   },
   {
      "x":25,
      "y":6,
      "z":119,
      "r":0,
      "g":1,
      "b":0,
      "options":{
         "name":"Green"
      }
   },
   {
      "x":48,
      "y":5,
      "z":110,
      "r":0,
      "g":1,
      "b":0,
      "options":{
         "name":"Blue"
      }
   }
]

§c§lThe §6§lFLAME §c§ldragon is spawning!
§c§lThe §a§lAPEX §c§ldragon is spawning!
§c§lThe §c§lPOWER §c§ldragon is spawning!
§c§lThe §b§lICE §c§ldragon is spawning!
§c§lThe §5§lSOUL §c§ldragon is spawning!
 */


data class ArrowStackLocation(val witheredDragonInfo: WitheredDragonInfo, val location: LorenzVec)

@SkyHanniModule
object ArrowStackWaypoints {

    val locations = arrayOf( // +1 y
        ArrowStackLocation(WitheredDragonInfo.ICE, LorenzVec(48, 6, 110)),
        ArrowStackLocation(WitheredDragonInfo.APEX, LorenzVec(25, 7, 119)),
        ArrowStackLocation(WitheredDragonInfo.FLAME, LorenzVec(53, 5, 90)),
        ArrowStackLocation(WitheredDragonInfo.POWER, LorenzVec(18, 6, 84)),
        ArrowStackLocation(WitheredDragonInfo.POWER, LorenzVec(10, 7, 83)),
        ArrowStackLocation(WitheredDragonInfo.SOUL, LorenzVec(31, 6, 97)),
        ArrowStackLocation(WitheredDragonInfo.SOUL, LorenzVec(81, 6, 99))
    )

    var closestLocation: ArrowStackLocation? = null
    var currentDragon: WitheredDragonInfo? = null
    var shouldTracerSpawnLocation = false
    var disableChecks = false // TODO: remove
    var overrideClass: DungeonAPI.DungeonClass? = null // TODO: adjust to pr

    // TODO: Only bers and arch setting
    // TODO: Custom Prio settings
    // TODO: enable and disable feature

    @HandleEvent
    fun onDragonSpawning(event: WitheredDragonEvent.ChangeEvent) {
        if (!inDungeon() && !disableChecks) return // TODO: remove disableChecks
        ChatUtils.chat("${event.dragon.color.toChatFormatting()}${event.dragon.name} - ${event.state}") // TODO: remove
        if (event.state != WitheredDragonSpawnedStatus.SPAWNING) return
        // FIXME: (currentDragon?.defeated?.not() == true &&
        //                 (currentDragon?.status == M7SpawnedStatus.ALIVE || currentDragon?.status != M7SpawnedStatus.SPAWNING))
        ChatUtils.chat("Spawning: ${event.dragon.name}")
        
        val values = WitheredDragonInfo.entries.filter { it.status == WitheredDragonSpawnedStatus.SPAWNING && !it.defeated }
        ChatUtils.chat(values.joinToString(" ") { it.name })
        val sorted = values.sortedBy { it.ordinal }
        val dragWithHighestPrio = when (overrideClass ?: DungeonAPI.playerClass) {
            DungeonAPI.DungeonClass.BERSERK, DungeonAPI.DungeonClass.MAGE -> sorted.first()
            else -> sorted.last()
        }

        ChatUtils.chat(dragWithHighestPrio.colorName)

        ChatUtils.chat(WitheredDragonInfo.entries.filter { it.status == WitheredDragonSpawnedStatus.SPAWNING }.joinToString(" ") { it.name })
        ChatUtils.chat(WitheredDragonInfo.entries.filter { !it.defeated }.joinToString(" "))

        if (dragWithHighestPrio == currentDragon) return

        shouldTracerSpawnLocation = false

        currentDragon = dragWithHighestPrio

        closestLocation = locations
            .filter { it.witheredDragonInfo == dragWithHighestPrio }
            .maxBy { it.location.distance(Minecraft.getMinecraft().thePlayer.position.toLorenzVec()) }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!inDungeon() && !disableChecks && currentDragon != null) return // TODO: remove disableChecks
        if (closestLocation != null && !shouldTracerSpawnLocation) {
            event.drawColor(closestLocation!!.location, closestLocation!!.witheredDragonInfo.color, true, 0.4f)
            event.draw3DLine(
                event.exactPlayerEyeLocation(),
                closestLocation!!.location.add(0.5, 0.5, 0.5),
                closestLocation!!.witheredDragonInfo.color.toColor(),
                2,
                false
            )
        }
        if (shouldTracerSpawnLocation) {
            event.drawColor(currentDragon!!.spawnLocation, currentDragon!!.color, false, 0.4f)
            event.draw3DLine(
                event.exactPlayerEyeLocation(),
                currentDragon!!.spawnLocation.add(0.5, 0.5, 0.5),
                currentDragon!!.color.toColor(),
                2,
                false
            )
        }
    }

    @HandleEvent
    fun onDragonChange(event: WitheredDragonEvent.ChangeEvent) {
        if (!inDungeon() && !disableChecks) return
        if (event.previous != WitheredDragonSpawnedStatus.SPAWNING || event.dragon != currentDragon) return
        // currentDragon changes from spawning to alive
        currentDragon = null
        closestLocation = null
        shouldTracerSpawnLocation = false
    }

    @SubscribeEvent
    fun onTick(event: SecondPassedEvent) {
        if ((!inDungeon() && !disableChecks) || currentDragon == null || shouldTracerSpawnLocation) return

        val playerPos = Minecraft.getMinecraft().thePlayer.position.toLorenzVec()
        val distance = playerPos.distance(closestLocation!!.location)
        if (distance < 1.5) {
            shouldTracerSpawnLocation = true
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: LorenzWorldChangeEvent) {
        closestLocation = null
        currentDragon = null
    }

    fun inDungeon(): Boolean {
        if (!DungeonAPI.inDungeon()) return false
        if (!DungeonAPI.inBossRoom) return false
        if (!DungeonAPI.isOneOf("M7")) return false
        return true
    }
}
