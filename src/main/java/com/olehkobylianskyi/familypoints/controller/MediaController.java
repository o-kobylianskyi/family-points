package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.MediaService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}/media")
public class MediaController {
    private final MediaService media;
    public MediaController(MediaService media){this.media=media;}
    public record BindRequest(String assetId,String catalogKey,Integer expectedVersion){}
    @GetMapping("/catalog")
    public List<MediaService.CatalogItem> catalog(@PathVariable long workspaceId) {
        // Does not reveal private workspace data; still require selection via the same media API.
        media.assertCatalogWorkspace(workspaceId);
        return media.catalog();
    }
    @PostMapping(value="/uploads",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaService.Uploaded upload(@PathVariable long workspaceId,@RequestPart("file") MultipartFile file){
        return media.upload(workspaceId,file);
    }
    @GetMapping("/bindings/{type}/{targetId}/{slot}")
    public MediaService.Ref effective(@PathVariable long workspaceId,@PathVariable String type,
            @PathVariable long targetId,@PathVariable String slot){
        return media.effective(workspaceId,type,targetId,slot);
    }
    @PutMapping("/bindings/{type}/{targetId}/{slot}")
    public MediaService.Ref bind(@PathVariable long workspaceId,@PathVariable String type,
            @PathVariable long targetId,@PathVariable String slot,@RequestBody BindRequest request){
        return media.bind(workspaceId,type,targetId,slot,request.assetId(),request.catalogKey(),request.expectedVersion());
    }
    @DeleteMapping("/bindings/{type}/{targetId}/{slot}")
    public MediaService.Ref clear(@PathVariable long workspaceId,@PathVariable String type,
            @PathVariable long targetId,@PathVariable String slot,
            @RequestParam Integer expectedVersion){
        return media.clear(workspaceId,type,targetId,slot,expectedVersion);
    }
    @GetMapping("/{assetId}/variants/{variant}")
    public ResponseEntity<byte[]> read(@PathVariable long workspaceId,@PathVariable String assetId,
            @PathVariable String variant){
        var result=media.getBoundVariant(workspaceId,assetId,variant);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(result.mime()))
                .header("X-Content-Type-Options","nosniff")
                .header(HttpHeaders.CACHE_CONTROL,"private, no-store")
                .body(result.bytes());
    }
}
