package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.MediaService;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaManifestControllerTest {
    @Test void manifestUsesOwnWorkspaceOnlyAndExcludesAvatarChoices() {
        var service=mock(MediaService.class);
        var controller=new MediaController(service);
        var cover=new MediaService.Ref("DEFAULT",null,"family-cover-default",0,null);
        var icon=new MediaService.Ref("OWN",null,"family-default",2,
                "/workspaces/42/media/catalog/family-default/preview");
        when(service.effective(42L,"WORKSPACE",42L,"COVER")).thenReturn(cover);
        when(service.effective(42L,"WORKSPACE",42L,"ICON")).thenReturn(icon);
        when(service.catalog()).thenReturn(List.of(
                new MediaService.CatalogItem("family-cover-default","COVER","media.catalog.familyCoverDefault",1,true,"/cover"),
                new MediaService.CatalogItem("family-default","FAMILY","media.catalog.familyDefault",1,true,"/family"),
                new MediaService.CatalogItem("avatar-sky","AVATAR","media.catalog.avatarSky",1,true,"/avatar")));
        var manifest=controller.manifest(42L);
        assertSame(cover,manifest.cover());
        assertSame(icon,manifest.icon());
        assertEquals(2,manifest.coverChoices().size());
        assertTrue(manifest.coverChoices().stream().noneMatch(x->x.category().equals("AVATAR")));
        verify(service).effective(42L,"WORKSPACE",42L,"COVER");
        verify(service).effective(42L,"WORKSPACE",42L,"ICON");
        verify(service).catalog();
        verifyNoMoreInteractions(service);
    }

    @Test
    void authenticatedCatalogPreviewIsNotStoredInSharedPublicCaches() {
        var service=mock(MediaService.class);
        var controller=new MediaController(service);
        byte[] image="<svg/>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(service.catalogPreview(42L,"family-default")).thenReturn(image);
        var response=controller.catalogPreview(42L,"family-default");
        assertEquals("private, no-store",
                response.getHeaders().getFirst(org.springframework.http.HttpHeaders.CACHE_CONTROL));
        assertArrayEquals(image,response.getBody());
        verify(service).catalogPreview(42L,"family-default");
    }

}
