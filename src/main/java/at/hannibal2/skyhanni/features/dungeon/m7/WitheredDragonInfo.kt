package at.hannibal2.skyhanni.features.dungeon.m7

import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.util.AxisAlignedBB

enum class WitheredDragonInfo(
    val colorName: String,
    val isEasy: Boolean,
    val color: LorenzColor,
    val particleBox: AxisAlignedBB,
    val deathBox: AxisAlignedBB,
    val spawnLocation: LorenzVec,
    var status: WitheredDragonSpawnedStatus = WitheredDragonSpawnedStatus.DEAD,
    var defeated: Boolean = false,
    var id: Int? = null,
) {
    FLAME(
        "Orange",
        true,
        LorenzColor.GOLD,
        AxisAlignedBB(82.0, 10.0, 53.0, 88.0, 25.0, 59.0),
        AxisAlignedBB(70.0, 8.0, 47.0, 102.0, 28.0, 77.0),
        LorenzVec(84.0, 14.0, 56.0),
    ),
    APEX(
        "Green",
        true,
        LorenzColor.GREEN,
        AxisAlignedBB(23.0, 10.0, 91.0, 29.0, 25.0, 97.0),
        AxisAlignedBB(7.0, 8.0, 80.0, 37.0, 28.0, 110.0),
        LorenzVec(27.0, 14.0, 94.0),
    ),
    POWER(
        "Red",
        false,
        LorenzColor.RED,
        AxisAlignedBB(24.0, 10.0, 56.0, 30.0, 25.0, 62.0),
        AxisAlignedBB(14.0, 13.0, 46.0, 39.0, 28.0, 71.0),
        LorenzVec(27.0, 14.0, 58.0),
    ),
    ICE(
        "Blue",
        false, // Split Prio :: B/M --> OGRBP <-- A/H/T
        LorenzColor.AQUA,
        AxisAlignedBB(82.0, 10.0, 91.0, 88.0, 25.0, 97.0),
        AxisAlignedBB(71.5, 16.0, 82.5, 96.5, 26.0, 107.5),
        LorenzVec(84.0, 14.0, 94.0),
    ),
    SOUL(
        "Purple",
        true,
        LorenzColor.DARK_PURPLE,
        AxisAlignedBB(53.0, 10.0, 122.0, 59.0, 25.0, 128.0),
        AxisAlignedBB(45.5, 13.0, 113.5, 68.5, 23.0, 136.5),
        LorenzVec(56.0, 14.0, 125.0),
    );

    companion object {
        fun clearSpawned() {
            entries.forEach {
                it.status = WitheredDragonSpawnedStatus.DEAD
                it.id = null
            }
        }

        fun getClosestSpawn(input: LorenzVec): WitheredDragonInfo? {
            return entries.minByOrNull { it.spawnLocation.distance(input) }
        }
    }
}

enum class WitheredDragonSpawnedStatus {
    DEAD,
    SPAWNING,
    ALIVE,
}
