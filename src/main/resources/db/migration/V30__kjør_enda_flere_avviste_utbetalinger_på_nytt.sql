UPDATE utbetaling
SET neste_paminnelsetidspunkt=now()
WHERE id IN (
             '17d56979-f60c-40ce-8d37-97bdb7b77dc1',
             '1c7bdddd-12a2-4382-96ac-1faaddf681bc',
             'b5d04157-6968-4914-bbb2-c30a3b68c909',
             '4e0f9a66-c43c-4e30-a896-9087b12e0f90',
             '38025592-c2ef-4d7e-87ba-59c5a4edf982',
             'b1b382fa-f610-4f96-80ea-a628ba33e0eb',
             '69bab99a-fd0c-4c1e-b414-6fd707a8a92c',
             '393624f8-589a-4162-9d71-db170dc40609',
             '38a54df6-c321-4ab4-9ec7-74b9b32db0dc',
             '9fd23c60-b645-4544-a9e8-0537d016eab9',
             '26357b81-783a-4e22-8b1d-215b5c4b8cd4'
    );