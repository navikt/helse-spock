# opprydding-dev

Egen Gradle-modul for opprydding knyttet til Spock-databasen, inspirert av `sp-forsikring`. Inneholder:

- egen Gradle-submodule (`:opprydding-dev`)
- egen `App.kt`
- egen river/listener som reagerer på et event
- egen datakilde til databasen
- egen Docker-image, NAIS-app og GitHub Actions-workflow

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

```
./gradlew :opprydding-dev:run
```

Krever samme database-miljøvariabler som selve `spock`-appen (`DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_DATABASE`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, eller `DATABASE_JDBC_URL`).

## Teste

```
./gradlew :opprydding-dev:test
```

Testene bruker Testcontainers (krever Docker) og gjenbruker databasemigreringene fra `:spock`-modulen, siden `opprydding-dev` opererer på samme databaseskjema.

## Bygge og deploye

Bygging, testing og deploy til dev skjer via `.github/workflows/opprydding-dev.yml`, som trigges av endringer i:
- `opprydding-dev/**`
- `deploy/opprydding-dev.yml`
- `deploy/dev-db-policy.yml`

Appen deployes som `spock-opprydding-dev` i namespace `tbd`.
