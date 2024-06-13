package at.hannibal2.skyhanni.features.dungeon.m7

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.DungeonM7Phase5Start
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.M7DragonChangeEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
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

        val matchedDragon = M7DragonInfo.entries.firstOrNull { it.dragonLocation.particleBox.isInside(location) }
        if (matchedDragon == null) {
            logLine("[Spawn] dragon ${event.mob.baseEntity.entityId}, '${location.toCleanString()}', no spawn matched")
            return
        }

        M7DragonChangeEvent(matchedDragon, M7SpawnedStatus.ALIVE).post()
        logSpawn(event.mob, matchedDragon)

        matchedDragon.status = M7SpawnedStatus.ALIVE
        matchedDragon.status.id = event.mob.baseEntity.entityId
    }

    @SubscribeEvent
    fun onDragonKill(event: MobEvent.DeSpawn.SkyblockMob) {
        if (!isEnabled()) return
        if (event.mob.baseEntity !is EntityDragon) return
        if (event.mob.mobType != Mob.Type.BOSS) return
        if (event.mob.name != "Withered Dragon") return

        val location = event.mob.baseEntity.position.toLorenzVec()
        val id = event.mob.baseEntity.entityId
        val matchedDragon = M7DragonInfo.entries.firstOrNull { it.status.id == id }
        if (matchedDragon == null) {
            logLine("dragon $id died, no matched dragon")
            return
        }
        val status = if (matchedDragon.dragonLocation.deathBox.isInside(location)) M7SpawnedStatus.DEFEATED
        else M7SpawnedStatus.UNDEFEATED
        M7DragonChangeEvent(matchedDragon, status)

        matchedDragon.status = status
        logKill(event.mob, matchedDragon)

        matchedDragon.status.id = -1
    }

    @HandleEvent
    fun onParticles(event: PacketReceivedEvent) {
        if (!isEnabled()) return
        if (event.packet !is S2APacketParticles) return

        val particle = event.packet
        if (!checkParticle(particle)) return
        val location = particle.toLorenzVec()

        val matchedDragon = M7DragonInfo.entries.firstOrNull { it.dragonLocation.particleBox.isInside(location) }
        logParticle(particle, matchedDragon)
        if (matchedDragon == null) return

        M7DragonChangeEvent(matchedDragon, M7SpawnedStatus.SPAWNING).post()
        matchedDragon.status = M7SpawnedStatus.SPAWNING
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
        M7DragonInfo.clearSpawned()
        if (inPhase5) inPhase5 = false
    }

    @SubscribeEvent
    fun onLeave(event: LorenzWorldChangeEvent) {
        M7DragonInfo.clearSpawned()
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
            add("Power: ${M7DragonInfo.POWER.status}, ${M7DragonInfo.POWER.status.id}")
            add("Flame: ${M7DragonInfo.FLAME.status}, ${M7DragonInfo.FLAME.status.id}")
            add("Apex: ${M7DragonInfo.APEX.status}, ${M7DragonInfo.APEX.status.id}")
            add("Ice: ${M7DragonInfo.ICE.status}, ${M7DragonInfo.ICE.status.id}")
            add("Soul: ${M7DragonInfo.SOUL.status}, ${M7DragonInfo.SOUL.status.id}")
        }
    }

    private fun logParticle(particle: S2APacketParticles, matchedType: M7DragonInfo?) {
        val location = particle.toLorenzVec()

        var string = "[Particle] $location"
        string += if (matchedType != null) {
            ", matched $matchedType"
        } else {
            ", did not match"
        }

        logLine(string)
    }

    private fun logSpawn(mob: Mob, matchedType: M7DragonInfo?) {
        val location = mob.baseEntity.position.toLorenzVec()

        var string = "[Spawn] $location, ${mob.baseEntity.entityId}"
        string += if (matchedType != null) {
            ", matched $matchedType"
        } else {
            ", did not match"
        }
        logLine(string)
    }

    private fun logKill(mob: Mob, matchedType: M7DragonInfo?) {
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
        M7DragonInfo.entries.forEach {
            event.drawFilledBoundingBox_nea(it.dragonLocation.particleBox, it.color.toColor().addAlpha(100))
            event.drawWaypointFilled(it.dragonLocation.spawnLocation, it.color.toColor(), true)
            event.drawString(it.dragonLocation.spawnLocation.add(y = 1), it.colorName, true)
        }
    }
}
