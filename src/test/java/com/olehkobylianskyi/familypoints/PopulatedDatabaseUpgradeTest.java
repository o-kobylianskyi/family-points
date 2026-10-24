package com.olehkobylianskyi.familypoints;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises a two-step Flyway migration on a real PostgreSQL 17 database.
 * This is a disposable populated-DB surrogate; it never connects to BETA.
 */
@Testcontainers
class PopulatedDatabaseUpgradeTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("family_points_upgrade_test")
            .withUsername("fp_upgrade")
            .withPassword("fp_upgrade_password");

    private static Flyway flyway(boolean baselineOnly) {
        var config = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration");
        if (baselineOnly) config.target(MigrationVersion.fromVersion("35"));
        return config.load();
    }

    private static long insertReturningId(Connection connection, String query) throws Exception {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query + " RETURNING id")) {
            assertTrue(rs.next());
            return rs.getLong(1);
        }
    }

    private static long scalar(Connection connection, String sql) throws Exception {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            assertTrue(rs.next());
            return rs.getLong(1);
        }
    }

    @Test
    void upgradeV35ToCalendarV36AndMediaV37PreservesExistingFamilyRewardAndLedger() throws Exception {
        assertEquals(35, flyway(true).migrate().migrationsExecuted);
        long familyId;
        long rewardId;
        long ledgerId;
        try (Connection db = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            familyId = insertReturningId(db,
                    "INSERT INTO families(name) VALUES ('Populated upgrade family')");
            long roleId = insertReturningId(db,
                    "INSERT INTO family_roles(family_id,system_role,code,name) VALUES (" +
                            familyId + ",true,'PARENT','Parent')");
            long pointsId = insertReturningId(db,
                    "INSERT INTO point_types(family_id,active,sort_order,code,name) VALUES (" +
                            familyId + ",true,1,'POINTS','Points')");
            long memberId = insertReturningId(db,
                    "INSERT INTO family_members(family_id,family_role_id,active,member_type,name) VALUES (" +
                            familyId + "," + roleId + ",true,'PARENT','Existing member')");
            ledgerId = insertReturningId(db,
                    "INSERT INTO point_transactions(member_id,point_type_id,amount,created_at,type,source_type,description) " +
                            "VALUES (" + memberId + "," + pointsId +
                            ",15,now(),'EARN','MANUAL','Existing balance')");
            rewardId = insertReturningId(db,
                    "INSERT INTO reward_definitions(family_id,title,point_type_id,price_amount,requires_approval) " +
                            "VALUES (" + familyId + ",'Existing reward'," + pointsId + ",20,true)");
        }

        var migrationResult = flyway(false).migrate();
        assertEquals(2, migrationResult.migrationsExecuted, "Only V36 and V37 may run during upgrade");

        try (Connection db = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            assertEquals(1, scalar(db, "SELECT count(*) FROM families WHERE id=" + familyId +
                    " AND name='Populated upgrade family' AND time_zone='UTC'"));
            assertEquals(1, scalar(db, "SELECT count(*) FROM reward_definitions WHERE id=" + rewardId +
                    " AND title='Existing reward' AND price_amount=20 AND availability_mode='ALWAYS'"));
            assertEquals(1, scalar(db, "SELECT count(*) FROM point_transactions WHERE id=" + ledgerId +
                    " AND source_type='MANUAL' AND amount=15"));
            assertEquals(3, scalar(db,
                    "SELECT count(*) FROM flyway_schema_history WHERE version IN ('35','36','37') AND success=true"));
            assertEquals(3, scalar(db,
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' " +
                    "AND table_name IN ('media_assets','media_blobs','media_bindings')"));
            assertEquals(1, scalar(db,
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' " +
                    "AND table_name='reward_weekly_windows'"));
        }
        // A second migration must be a no-op, not recreate tables or corrupt legacy data.
        assertEquals(0, flyway(false).migrate().migrationsExecuted);
    }
}
