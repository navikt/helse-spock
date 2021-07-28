UPDATE paminnelse
SET neste_paminnelsetidspunkt = now()
WHERE vedtaksperiode_id IN (
        '223c0ec3-c68d-409a-be3c-644edfa28bf0',
        '7ebbe58a-7f20-4271-819b-0ddb2969a1e9',
        '350e9fb2-7fe7-41e2-9d0f-203c83d25536'
    )