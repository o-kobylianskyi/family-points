package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaCatalogServiceTest {
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final MediaService service = new MediaService(mock(JdbcTemplate.class), current);

    @Test void everyGlobalCatalogChoiceHasItsOwnRenderableBundledImage() {
        when(current.belongsToWorkspace(7L)).thenReturn(true);
        var keys = new HashSet<String>();
        for (var item : service.catalog()) {
            assertTrue(keys.add(item.key()), "Duplicate global catalog key: "+item.key());
            assertTrue(item.selectable());
            assertNotNull(item.previewPath());
            assertEquals(1, item.version());
            assertTrue(item.previewPath().contains("/media/catalog/"+item.key()+"/preview"));
            String svg = new String(service.catalogPreview(7L,item.key()), StandardCharsets.UTF_8);
            assertTrue(svg.startsWith("<svg"), item.key());
            assertTrue(svg.contains("viewBox="), item.key());
            assertFalse(svg.contains("<script"), item.key());
            assertFalse(svg.contains("<foreignObject"), item.key());
            assertFalse(svg.contains("http://"), item.key());
        }
        assertTrue(keys.size() >= 15);
    }

    @Test void catalogPreviewRejectsForeignFamilyEvenForPubliclyReusableKey() {
        when(current.belongsToWorkspace(9L)).thenReturn(false);
        var denied = assertThrows(ResponseStatusException.class,
                () -> service.catalogPreview(9L, "avatar-sky"));
        assertEquals(HttpStatus.FORBIDDEN,denied.getStatusCode());
    }

    @Test void arbitrarySvgResourcePathCannotBeRequested() {
        when(current.belongsToWorkspace(7L)).thenReturn(true);
        var denied = assertThrows(ResponseStatusException.class,
                () -> service.catalogPreview(7L, "../../application"));
        assertEquals(HttpStatus.NOT_FOUND,denied.getStatusCode());
    }
}
