package no.nav.helse.spock

// Understands how to remind states
internal class Påminnelser {
    private val events = mutableListOf<TilstandsendringEvent>()

    fun håndter(event: TilstandsendringEvent): List<TilstandsendringEvent> {
        events.håndter(event)
        return påminnelser()
    }

    internal fun påminnelser(): List<TilstandsendringEvent> {
        return events.påminnelser()
    }

    private fun List<TilstandsendringEvent>.påminnelser() = this.fold(mutableListOf<TilstandsendringEvent>()) { påminnelser, event ->
        påminnelser.also { event.addWhenDue(it) }
    }
}
