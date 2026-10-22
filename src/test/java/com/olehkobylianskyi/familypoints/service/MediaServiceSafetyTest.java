package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
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
        var file=new MockMultipartFile("file","avatar.png","image/png",
                "not a real png".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class,()->media.upload(123L,file));
        verifyNoInteractions(db);
    }

    @Test void limitsOversizedUploadBeforeDecodeOrDatabaseWrite() {
        when(current.belongsToWorkspace(123L)).thenReturn(true);
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
}
