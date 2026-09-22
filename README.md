# Spock
[![main - spock](https://github.com/navikt/helse-spock/actions/workflows/main-spock.yml/badge.svg)](https://github.com/navikt/helse-spock/actions/workflows/main-spock.yml)
[![main - opprydding-dev](https://github.com/navikt/helse-spock/actions/workflows/main-opprydding-dev.yml/badge.svg)](https://github.com/navikt/helse-spock/actions/workflows/main-opprydding-dev.yml)

## Beskrivelse
Sender påminnelser for vedtaksperioder som er i en tilstand de kanskje kan komme seg ut av.

Er også ansvarlig for å starte avstemming av persondata. Avstemming av persondata er at Spleis sender ut info om personer som resten av appene kan synke mot.

## Moduler
Repoet er satt opp som et multi-modul Gradle-prosjekt:
- `spock/` – selve applikasjonen
- `opprydding-dev/` – frittstående app for opprydding/sletting av persondata i dev, se egen [README](opprydding-dev/README.md)

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #område-helse.
