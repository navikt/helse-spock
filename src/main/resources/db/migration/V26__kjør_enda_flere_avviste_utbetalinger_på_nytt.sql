UPDATE utbetaling
SET neste_paminnelsetidspunkt=now()
WHERE id IN
      (
       '368f0421-f389-49fe-978c-ed63e64f94d2',
       '4c88f7ef-8e91-45f7-88dc-8fa32e30fef4',
       'e21c98b1-2525-4419-a79a-2ee523af21f1',
       'a91c9855-4d54-47a5-9ff2-efd2f8aaf14d',
       '252a3ca9-277a-4b13-9906-9461635f342d',
       'f76eeea4-10e2-4d42-94f6-8248a93aedb8',
       '54106d5e-eed2-43a8-bfb6-a3bf428ad4c6',
       '61692c3a-3fff-48f5-84e4-7dd3fded48e4',
       'af133cd6-a833-4ed7-9b11-da1d60180ce5'
          );