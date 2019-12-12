package no.nav.helse.spock

// Understands how to remind states
internal class Påminnelser {
    private val events = mutableListOf<TilstandsendringEvent>()

    fun håndter(event: TilstandsendringEvent): List<TilstandsendringEvent.Påminnelse> {
        events.håndter(event)
        return events.påminnelser()
    }

    private fun List<TilstandsendringEvent>.påminnelser() = this.fold(mutableListOf<TilstandsendringEvent.Påminnelse>()) { påminnelser, event ->
        påminnelser.also { event.addWhenDue(it) }
    }
}
