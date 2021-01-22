UPDATE paminnelse SET neste_paminnelsetidspunkt = now() + INTERVAL '5 minutes'
WHERE tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP'
  AND neste_paminnelsetidspunkt >= date '9998-01-01';
