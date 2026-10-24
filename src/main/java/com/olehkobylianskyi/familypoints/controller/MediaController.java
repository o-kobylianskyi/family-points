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
    public record WorkspaceMediaManifest(MediaService.Ref cover, MediaService.Ref icon,
                                         List<MediaService.CatalogItem> coverChoices) {}

    @GetMapping("/manifest")
    public WorkspaceMediaManifest manifest(@PathVariable long workspaceId) {
        var cover=media.effective(workspaceId,"WORKSPACE",workspaceId,"COVER");
        var icon=media.effective(workspaceId,"WORKSPACE",workspaceId,"ICON");
        var choices=media.catalog().stream()
                .filter(x -> x.category().equals("COVER") || x.category().equals("FAMILY"))
                .toList();
        return new WorkspaceMediaManifest(cover,icon,choices);
    }

    @GetMapping("/catalog")
    public List<MediaService.CatalogItem> catalog(@PathVariable long workspaceId) {
        // Does not reveal private workspace data; still require selection via the same media API.
        media.assertCatalogWorkspace(workspaceId);
        return media.catalog();
    }
    @GetMapping(value="/catalog/{key}/preview", produces="image/svg+xml")
    public ResponseEntity<byte[]> catalogPreview(@PathVariable long workspaceId, @PathVariable String key) {
        byte[] image=media.catalogPreview(workspaceId,key);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .header("X-Content-Type-Options","nosniff")
                .header(HttpHeaders.CACHE_CONTROL,"public, max-age=86400")
                .body(image);
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
