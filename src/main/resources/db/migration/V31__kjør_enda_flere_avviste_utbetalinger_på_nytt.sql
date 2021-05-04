UPDATE utbetaling
SET neste_paminnelsetidspunkt=now()
WHERE id IN (
             'bf8d5ed1-c4f2-400b-896b-717d3f595f86',
             '7d540021-8562-4cbd-b21e-40f00b142208',
             'ae7838b6-3261-4073-9a90-6d4ccc3b0c08',
             'f95d11ed-abd3-412a-8f3d-317223b7d6be'
    );