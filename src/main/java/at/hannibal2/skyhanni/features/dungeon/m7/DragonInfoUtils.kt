package at.hannibal2.skyhanni.features.dungeon.m7

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.DungeonM7Phase5Start
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.WitheredDragonEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object DragonInfoUtils {
    private var inPhase5 = false
    private val logger = LorenzLogger("dragons")

    @SubscribeEvent
    fun onDragonSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!isEnabled()) return
        if (event.mob.baseEntity !is EntityDragon) return
        if (event.mob.mobType != Mob.Type.BOSS) return
        if (event.mob.name != "Withered Dragon") return

        val location = event.mob.baseEntity.position.toLorenzVec()
        val id = event.mob.baseEntity.entityId

        val matchedDragon = WitheredDragonInfo.entries.firstOrNull { it.particleBox.isInside(location) }
        if (matchedDragon == null) {
            logLine("[Spawn] dragon ${id}, '${location.toCleanString()}', no spawn matched")
            ChatUtils.debug("Unknown dragon $id spawned at ${location.toCleanString()}")
            return
        }

        logSpawn(event.mob, matchedDragon)

        matchedDragon.status = M7SpawnedStatus.ALIVE
        WitheredDragonEvent.ChangeEvent(matchedDragon, M7SpawnedStatus.ALIVE, matchedDragon.status).post()
        matchedDragon.id = id
    }

    @SubscribeEvent
    fun onDragonKill(event: MobEvent.DeSpawn.SkyblockMob) {
        if (!isEnabled()) return
        if (event.mob.baseEntity !is EntityDragon) return
        if (event.mob.mobType != Mob.Type.BOSS) return
        if (event.mob.name != "Withered Dragon") return

        val location = event.mob.baseEntity.position.toLorenzVec()
        val id = event.mob.baseEntity.entityId
        val matchedDragon = WitheredDragonInfo.entries.firstOrNull { it.id == id }
        if (matchedDragon == null) {
            logLine("dragon $id died, no matched dragon")
            ChatUtils.debug("Unknown dragon $id died at ${location.toCleanString()}")
            return
        }

        val defeated = matchedDragon.deathBox.isInside(location)

        matchedDragon.defeated = defeated
        WitheredDragonEvent.DefeatEvent(matchedDragon)
        logKill(event.mob, matchedDragon)

        matchedDragon.id = null
    }

    @HandleEvent
    fun onParticles(event: PacketReceivedEvent) {
        if (!isEnabled()) return
        if (event.packet !is S2APacketParticles) return

        val particle = event.packet
        if (!checkParticle(particle)) return
        val location = particle.toLorenzVec()

        val matchedDragon = WitheredDragonInfo.entries.firstOrNull { it.particleBox.isInside(location) }
        logParticle(particle, matchedDragon)
        if (matchedDragon == null) return

        matchedDragon.status = M7SpawnedStatus.SPAWNING
        WitheredDragonEvent.ChangeEvent(matchedDragon, M7SpawnedStatus.SPAWNING, matchedDragon.status).post()
    }

    private fun checkParticle(particle: S2APacketParticles): Boolean {
        return particle.run {
            particleType == EnumParticleTypes.FLAME &&
                particleCount == 20 &&
                xOffset == 2.0f &&
                yOffset == 3.0f &&
                zOffset == 2.0f &&
                isLongDistance &&
                (xCoordinate % 1) == 0.0 &&
                (yCoordinate % 1) == 0.0 &&
                (zCoordinate % 1) == 0.0
        }
    }

    @SubscribeEvent
    fun onStart(event: DungeonM7Phase5Start) {
        if (inPhase5) return
        logLine("------ run $currentRun -------")
        currentRun += 1
        logLine("Starting Phase5")
        inPhase5 = true
    }

    private var currentRun = 0

    @SubscribeEvent
    fun onEnd(event: DungeonCompleteEvent) {
        WitheredDragonInfo.clearSpawned()
        if (inPhase5) inPhase5 = false
    }

    @SubscribeEvent
    fun onLeave(event: LorenzWorldChangeEvent) {
        WitheredDragonInfo.clearSpawned()
        if (inPhase5) inPhase5 = false
    }

    @SubscribeEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("M7 Dragon Info")
        if (!isEnabled()) {
            event.addIrrelevant("not in phase5")
            return
        }

        event.addData {
            add("Power: ${WitheredDragonInfo.POWER.status}, ${WitheredDragonInfo.POWER.id}")
            add("Flame: ${WitheredDragonInfo.FLAME.status}, ${WitheredDragonInfo.FLAME.id}")
            add("Apex: ${WitheredDragonInfo.APEX.status}, ${WitheredDragonInfo.APEX.id}")
            add("Ice: ${WitheredDragonInfo.ICE.status}, ${WitheredDragonInfo.ICE.id}")
            add("Soul: ${WitheredDragonInfo.SOUL.status}, ${WitheredDragonInfo.SOUL.id}")
        }
    }

    private fun logParticle(particle: S2APacketParticles, matchedType: WitheredDragonInfo?) {
        val location = particle.toLorenzVec()

        var string = "[Particle] $location"
        string += if (matchedType != null) {
            ", matched $matchedType"
        } else {
            ", did not match"
        }

        logLine(string)
    }

    private fun logSpawn(mob: Mob, matchedType: WitheredDragonInfo?) {
        val location = mob.baseEntity.position.toLorenzVec()

        var string = "[Spawn] $location, ${mob.baseEntity.entityId}"
        string += if (matchedType != null) {
            ", matched $matchedType"
        } else {
            ", did not match"
        }
        logLine(string)
    }

    private fun logKill(mob: Mob, matchedType: WitheredDragonInfo?) {
        val location = mob.baseEntity.position.toLorenzVec()

        val baseEntity = mob.baseEntity
        baseEntity as EntityDragon
        var string = "[Death] $location, ${baseEntity.entityId}, ${baseEntity.animTime}"
        string += if (matchedType != null) {
            ", matched $matchedType, ${matchedType.status}"
        } else {
            ", did not match"
        }
        logLine(string)
    }

    private fun logLine(input: String) {
        logger.log(input)
        LorenzDebug.log(input)
    }

    fun isEnabled() = inPhase5 || PlatformUtils.isDevEnvironment

    @SubscribeEvent
    fun renderBoxes(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (!SkyHanniMod.feature.dev.debug.enabled) return
        WitheredDragonInfo.entries.forEach {
            event.drawFilledBoundingBox_nea(it.particleBox, it.color.toColor().addAlpha(100))
            event.drawWaypointFilled(it.spawnLocation, it.color.toColor(), true)
            event.drawString(it.spawnLocation.add(y = 1), it.colorName, true)
        }
    }

    @HandleEvent
    fun onDragonChange(event: WitheredDragonEvent.ChangeEvent) {
        ChatUtils.debug("${event.dragon} ${event.state}")
    }
}
