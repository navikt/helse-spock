UPDATE utbetaling
SET neste_paminnelsetidspunkt=now()
WHERE id IN
      (
       '466b92fc-2ecf-46b1-929c-c7941e62d532',
       'eddc91ee-bd8f-4aff-a47e-45f19f8f8cd8',
       'd8ffd2a3-ab28-4dce-b6ac-867b267b7bad',
       'd0313a8d-11dd-4bc7-9833-7a8a1b6536c2',
       '713d06ba-832e-43b9-8c8a-d8edcaec3d7f',
       'ee953aa7-a945-4d10-8e12-431d6b296e85'
          );