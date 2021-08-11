UPDATE paminnelse
SET neste_paminnelsetidspunkt=now()
WHERE neste_paminnelsetidspunkt > '2100-01-01'
  AND tilstand != 'TIL_ANNULLERING'
  AND opprettet > '2021-06-15'
  AND opprettet < '2021-08-12';
