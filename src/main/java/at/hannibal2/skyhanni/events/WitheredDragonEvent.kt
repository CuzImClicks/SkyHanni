package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.dungeon.m7.M7SpawnedStatus
import at.hannibal2.skyhanni.features.dungeon.m7.WitheredDragonInfo

class WitheredDragonEvent {
    class DefeatEvent(val dragon: WitheredDragonInfo) : SkyHanniEvent() // only called when the dragon is killed inside the box
    class ChangeEvent(val dragon: WitheredDragonInfo, val state: M7SpawnedStatus, val previous: M7SpawnedStatus) : SkyHanniEvent()
}
