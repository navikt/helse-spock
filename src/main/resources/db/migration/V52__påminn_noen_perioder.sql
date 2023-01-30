update paminnelse
set neste_paminnelsetidspunkt = now()
WHERE vedtaksperiode_id in (
    '7e17a167-ce28-4419-b9e2-5ecc3c881f01',
    '23ab70cf-ba40-423f-8d42-bf3cc52fa101',
    'dcc92dde-a120-42d0-ab28-93d956c4bd21',
    '50edf35c-8ac6-43d7-ac34-4374dd5a33f1'
    );
