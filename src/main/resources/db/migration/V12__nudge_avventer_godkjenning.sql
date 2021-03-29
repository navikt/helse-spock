UPDATE paminnelse SET neste_paminnelsetidspunkt = now()
WHERE tilstand='AVVENTER_GODKJENNING' and endringstidspunkt < (now() - interval '6 hours');