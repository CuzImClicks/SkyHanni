package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.dungeon.m7.WitheredDragonInfo
import at.hannibal2.skyhanni.features.dungeon.m7.WitheredDragonSpawnedStatus

class WitheredDragonEvent {
    class DefeatEvent(val dragon: WitheredDragonInfo) : SkyHanniEvent() // only called when the dragon is killed inside the box
    class ChangeEvent(
        val dragon: WitheredDragonInfo,
        val state: WitheredDragonSpawnedStatus,
        val previous: WitheredDragonSpawnedStatus
    ) : SkyHanniEvent()
}
