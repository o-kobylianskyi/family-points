package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.controller.WorkspaceTemplateController;
import com.olehkobylianskyi.familypoints.service.TemplateCatalogService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class WorkspacePrivateTemplatePostgresTest {
    @Container
    static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("family_points_templates")
            .withUsername("template_test")
            .withPassword("template_test_password");

    @Test
    void workspaceLibraryIsIsolatedAndDoesNotCreateTasksOrTransactions() {
        Flyway.configure().dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
                .locations("classpath:db/migration").load().migrate();
        var datasource = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(datasource);
        long a = jdbc.queryForObject("INSERT INTO families(name) VALUES ('Template family A') RETURNING id", Long.class);
        long b = jdbc.queryForObject("INSERT INTO families(name) VALUES ('Template family B') RETURNING id", Long.class);
        var controller = new WorkspaceTemplateController(jdbc, new TemplateCatalogService(),
                org.mockito.Mockito.mock(com.olehkobylianskyi.familypoints.repository.TaskDefinitionRepository.class),
                org.mockito.Mockito.mock(com.olehkobylianskyi.familypoints.repository.RewardDefinitionRepository.class),
                org.mockito.Mockito.mock(com.olehkobylianskyi.familypoints.controller.TaskController.class),
                org.mockito.Mockito.mock(com.olehkobylianskyi.familypoints.controller.RewardController.class));
        controller.create(a, new WorkspaceTemplateController.TemplateRequest("Adult walk", "{\"schedule\":\"WEEKLY\"}", "TASK"));
        assertEquals(1, controller.list(a).size());
        assertTrue(controller.list(b).isEmpty());
        long id = controller.list(a).get(0).id();
        assertThrows(ResponseStatusException.class, () -> controller.update(b,id,
                new WorkspaceTemplateController.TemplateRequest("Stolen", "{}", "TASK")));
        assertThrows(ResponseStatusException.class, () -> controller.delete(b,id));
        assertEquals("Adult walk", controller.list(a).get(0).title());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM task_definitions", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM reward_definitions", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM point_transactions", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM reward_purchases", Integer.class));
        var preview = controller.preview(a,new WorkspaceTemplateController.ImportRequest(List.of()));
        assertTrue(preview.toImport().isEmpty());
        controller.delete(a,id);
        assertTrue(controller.list(a).isEmpty());
    }
}
