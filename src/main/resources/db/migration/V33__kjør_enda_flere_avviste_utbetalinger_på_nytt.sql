UPDATE utbetaling
SET neste_paminnelsetidspunkt=now()
WHERE id IN (
             'cd247390-9456-4dce-87d5-c2e2787df43e',
             '39fb3f37-b9f4-4bc7-a295-c38bd01e7a2b',
             'e0f4c161-993f-4175-bb12-ab7d56d7f0e1',
             'b0f5836a-8bed-4917-b5bd-45659dd1dcab',
             '5a6492ff-0612-470c-a1b2-c94748708590',
             '4207e699-f4a5-4ee8-acc5-f29d9ab42795',
             '8f830c50-cfc1-4801-8d0d-a80cf8439662',
             'fea0178c-8df9-4191-8d0f-97bd59eed3ad'
    );