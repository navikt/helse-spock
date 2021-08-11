DELETE FROM paminnelse WHERE neste_paminnelsetidspunkt > '2100-01-01' AND tilstand = 'AVSLUTTET_UTEN_UTBETALING';
