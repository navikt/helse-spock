# opprydding-dev

Egen Gradle-modul for opprydding knyttet til Spock-databasen, inspirert av `sp-forsikring`. Inneholder:

- egen Gradle-submodule (`:opprydding-dev`)
- egen `App.kt`
- egen river/listener som reagerer på et event
- egen datakilde til databasen
- eget image (bygget med jib), egen NAIS-app og egen GitHub Actions-workflow

## Hva den gjør

Lytter på eventet `slett_person` og sletter all data for en person fra:
- `person`
- `paminnelse`
- `utbetaling`

Etter sletting publiseres eventet `person_slettet`.

Eksempel på event som trigger sletting:

```json
{
  "@event_name": "slett_person",
  "@id": "<uuid>",
  "fødselsnummer": "01020312345"
}
```

## Kjøre lokalt

Start `no.nav.helse.spock.opprydding_dev.AppKt` fra IDE-en. Appen krever enten
`DATABASE_SPOCK_OPPRYDDING_DEV_JDBC_URL`, eller `DATABASE_SPOCK_OPPRYDDING_DEV_HOST`,
`DATABASE_SPOCK_OPPRYDDING_DEV_PORT` og `DATABASE_SPOCK_OPPRYDDING_DEV_DATABASE`. I tillegg må
`DATABASE_SPOCK_OPPRYDDING_DEV_USERNAME` og `DATABASE_SPOCK_OPPRYDDING_DEV_PASSWORD` være satt.

## Teste

```
./gradlew :opprydding-dev:test
```

Testene bruker Testcontainers (krever Docker) og gjenbruker databasemigreringene fra `:spock`-modulen, siden `opprydding-dev` opererer på samme databaseskjema.

## Bygge og deploye

Bygging, testing og deploy til dev skjer via `.github/workflows/main-opprydding-dev.yml`, som trigges av endringer i koden og i:
- `deploy/opprydding-dev.yml`
- `deploy/dev-db-policy.yml`

Appen deployes som `spock-opprydding-dev` i namespace `tbd`.
