package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaServiceSafetyTest {
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final CurrentUserService current=mock(CurrentUserService.class);
    private final MediaService media=new MediaService(db,current);

    @Test void wrongWorkspaceRejectedBeforeReadingInput() {
        when(current.belongsToWorkspace(123L)).thenReturn(false);
        var file=new MockMultipartFile("file","avatar.png","image/png",new byte[]{1,2,3});
        var ex=assertThrows(ResponseStatusException.class,()->media.upload(123L,file));
        assertEquals(HttpStatus.FORBIDDEN,ex.getStatusCode());
        verifyNoInteractions(db);
    }

    @Test void rejectsSpoofedImageWithoutWritingToDatabase() {
        when(current.belongsToWorkspace(123L)).thenReturn(true);
        var actor=mock(WorkspaceMember.class);
        when(current.getCurrentMember()).thenReturn(actor);
        when(actor.hasPermission(WorkspacePermission.MANAGE_WORKSPACE)).thenReturn(true);
        var file=new MockMultipartFile("file","avatar.png","image/png",
                "not a real png".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class,()->media.upload(123L,file));
        verifyNoInteractions(db);
    }

    @Test void limitsOversizedUploadBeforeDecodeOrDatabaseWrite() {
        when(current.belongsToWorkspace(123L)).thenReturn(true);
        var actor=mock(WorkspaceMember.class);
        when(current.getCurrentMember()).thenReturn(actor);
        when(actor.hasPermission(WorkspacePermission.MANAGE_WORKSPACE)).thenReturn(true);
        var file=new MockMultipartFile("file","large.png","image/png",new byte[5*1024*1024+1]);
        assertThrows(IllegalArgumentException.class,()->media.upload(123L,file));
        verifyNoInteractions(db);
    }

    @Test void mediaBlobCannotBeReadWithoutWorkspaceMembership() {
        when(current.belongsToWorkspace(8L)).thenReturn(false);
        assertThrows(ResponseStatusException.class,()->
                media.getBoundVariant(8L,UUID.randomUUID().toString(),"THUMB"));
        verifyNoInteractions(db);
    }
    @Test void wideCoverKeepsAspectRatioWithoutUpscaling() throws Exception {
        var render=MediaService.class.getDeclaredMethod("render",java.awt.image.BufferedImage.class,int.class,String.class);
        render.setAccessible(true);
        var large=new java.awt.image.BufferedImage(3000,1200,java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int max : new int[]{2048,800,192}) {
            byte[] encoded=(byte[])render.invoke(null,large,max,"image/png");
            var output=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(encoded));
            assertNotNull(output);
            assertEquals(max,output.getWidth());
            assertEquals((int)Math.round(1200.0*max/3000),output.getHeight());
        }
        var tiny=new java.awt.image.BufferedImage(320,180,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var result=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(
                (byte[])render.invoke(null,tiny,2048,"image/png")));
        assertEquals(320,result.getWidth());
        assertEquals(180,result.getHeight());
    }

    @Test void invalidAssetIdGetsBadRequestWithoutDataAccess() {
        when(current.belongsToWorkspace(7L)).thenReturn(true);
        var error=assertThrows(ResponseStatusException.class, ()->
                media.getBoundVariant(7L,"not-a-valid-uuid","COVER"));
        assertEquals(HttpStatus.BAD_REQUEST,error.getStatusCode());
        verifyNoInteractions(db);
    }

    @Test void globalCoverOverrideCannotBindAnAvatarCatalogKey() {
        when(current.belongsToWorkspace(7L)).thenReturn(true);
        var manager=mock(WorkspaceMember.class);
        when(current.getCurrentMember()).thenReturn(manager);
        when(manager.hasPermission(WorkspacePermission.MANAGE_WORKSPACE)).thenReturn(true);
        when(db.queryForObject(anyString(),eq(Integer.class),eq(7L),eq(7L))).thenReturn(1);
        assertThrows(IllegalArgumentException.class, ()->
                media.bind(7L,"WORKSPACE",7L,"COVER",null,"avatar-sky",0));
    }
}
