DO
$$
BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spock_opprydding_dev')
        THEN
            GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO "spock_opprydding_dev";
            GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO "spock_opprydding_dev";
END IF;
END
$$;
DO
$$
BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spock_opprydding_dev')
        THEN
            ALTER DEFAULT PRIVILEGES FOR USER "spock" IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO "spock_opprydding_dev";
            ALTER DEFAULT PRIVILEGES FOR USER "spock" IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO "spock_opprydding_dev";
END IF;
END
$$;
