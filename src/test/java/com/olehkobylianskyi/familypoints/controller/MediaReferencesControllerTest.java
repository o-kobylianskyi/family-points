package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.MediaService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaReferencesControllerTest {
    private final MediaService media=mock(MediaService.class);
    private final MediaController controller=new MediaController(media);

    @Test void referencesSupportMemberGroupAndTaskInOneAuthorizedRequest() {
        var member=new MediaController.ImageTarget("MEMBER",11L,"AVATAR");
        var group=new MediaController.ImageTarget("MEMBER_GROUP",22L,"ICON");
        var task=new MediaController.ImageTarget("TASK",33L,"ICON");
        when(media.effective(7L,"MEMBER",11L,"AVATAR"))
                .thenReturn(new MediaService.Ref("DEFAULT",null,"member-default",0,null));
        when(media.effective(7L,"MEMBER_GROUP",22L,"ICON"))
                .thenReturn(new MediaService.Ref("OWN",null,"people-group-default",1,"/preview"));
        when(media.effective(7L,"TASK",33L,"ICON"))
                .thenReturn(new MediaService.Ref("INHERITED",null,"task-group-default",2,"/inherited"));
        var response=controller.references(7L,new MediaController.ReferenceBatch(List.of(member,group,task,task)));
        assertEquals(3,response.size());
        assertEquals("INHERITED",response.get(2).image().source());
        verify(media,times(1)).effective(7L,"TASK",33L,"ICON");
    }

    @Test void rejectsLargeOrEmptyBatchBeforeAccessingDatabase() {
        var target=new MediaController.ImageTarget("TASK",1L,"ICON");
        for(var values:List.of(List.<MediaController.ImageTarget>of(),
                java.util.Collections.nCopies(101,target))) {
            var ex=assertThrows(ResponseStatusException.class,
                    () -> controller.references(7L,new MediaController.ReferenceBatch(values)));
            assertEquals(HttpStatus.BAD_REQUEST,ex.getStatusCode());
        }
        verifyNoInteractions(media);
    }

    @Test void doesNotSwallowAuthorizationFailure() {
        var target=new MediaController.ImageTarget("MEMBER",11L,"AVATAR");
        when(media.effective(7L,"MEMBER",11L,"AVATAR"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        var error=assertThrows(ResponseStatusException.class,
                () -> controller.references(7L,new MediaController.ReferenceBatch(List.of(target))));
        assertEquals(HttpStatus.NOT_FOUND,error.getStatusCode());
    }
}
