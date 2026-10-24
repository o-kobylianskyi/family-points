package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.List;

@Service
public class MediaService {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 16_000_000L;
    private static final Set<String> TYPES = Set.of("WORKSPACE","MEMBER","MEMBER_GROUP","WORK_NODE","TASK");
    private static final Set<String> SLOTS = Set.of("COVER","ICON","AVATAR");
    private static final Set<String> CATALOG = Set.of("member-default","avatar-sunflower","avatar-sky","avatar-berry","avatar-mint","avatar-peach","avatar-violet","avatar-ocean","avatar-forest","family-default","family-cover-default","people-group-default","task-group-default","task-default","reward-star");
    private final JdbcTemplate db;
    private final CurrentUserService current;

    public MediaService(JdbcTemplate db, CurrentUserService current) {
        this.db = db;
        this.current = current;
    }

    public record Ref(String source, String mediaId, String catalogKey, int version, String path) {}
    public record ImageData(String mime, byte[] bytes) {}
    public record Uploaded(String assetId, String mime, int width, int height, int bytes) {}

    private void requireWorkspace(long workspace) {
        if (!current.belongsToWorkspace(workspace)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    private void requireManager(long workspace, String type, long id) {
        requireWorkspace(workspace);
        WorkspacePermission permission = switch(type) {
            case "WORKSPACE" -> WorkspacePermission.MANAGE_WORKSPACE;
            case "MEMBER" -> WorkspacePermission.MANAGE_MEMBERS;
            case "MEMBER_GROUP" -> WorkspacePermission.MANAGE_MEMBERS;
            case "WORK_NODE", "TASK" -> WorkspacePermission.MANAGE_TASKS;
            default -> throw new IllegalArgumentException("Invalid media target");
        };
        var member = current.getCurrentMember();
        if (type.equals("MEMBER") && member.getId().equals(id)) return; // own workspace avatar
        if (!member.hasPermission(permission) && !member.hasPermission(WorkspacePermission.ADMIN_OVERRIDE))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    private void checkTarget(long ws, String type, long id, String slot) {
        if (!TYPES.contains(type) || !SLOTS.contains(slot)) throw new IllegalArgumentException("Invalid media target");
        if (!(type.equals("WORKSPACE") && (slot.equals("COVER") || slot.equals("ICON"))
                || type.equals("MEMBER") && slot.equals("AVATAR")
                || (type.equals("MEMBER_GROUP") || type.equals("WORK_NODE") || type.equals("TASK")) && slot.equals("ICON")))
            throw new IllegalArgumentException("Invalid media slot for target");
        String sql = switch(type) {
            case "WORKSPACE" -> "SELECT count(*) FROM families WHERE id=? AND id=?";
            case "MEMBER" -> "SELECT count(*) FROM family_members WHERE id=? AND family_id=? AND active=true";
            case "MEMBER_GROUP" -> "SELECT count(*) FROM member_groups WHERE id=? AND family_id=? AND active=true";
            case "WORK_NODE" -> "SELECT count(*) FROM work_nodes WHERE id=? AND family_id=? AND active=true AND type='GROUP'";
            case "TASK" -> "SELECT count(*) FROM task_definitions WHERE id=? AND family_id=?";
            default -> throw new IllegalArgumentException("Invalid target");
        };
        if (db.queryForObject(sql, Integer.class, id, ws) != 1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media target not found");
    }

    @Transactional
    public Uploaded upload(long ws, MultipartFile file) {
        requireWorkspace(ws);
        var actor = current.getCurrentMember();
        // Keep the first version manager-only. An unrestricted unbound upload endpoint
        // lets any child exhaust database storage regardless of binding ACLs.
        if (!actor.hasPermission(WorkspacePermission.MANAGE_WORKSPACE)
                && !actor.hasPermission(WorkspacePermission.MANAGE_MEMBERS)
                && !actor.hasPermission(WorkspacePermission.MANAGE_TASKS)
                && !actor.hasPermission(WorkspacePermission.ADMIN_OVERRIDE))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Media upload permission required");
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES)
            throw new IllegalArgumentException("Image must be 1..5 MiB");
        byte[] input;
        try (InputStream in = file.getInputStream()) {
            input = in.readNBytes(MAX_BYTES + 1);
        } catch(IOException e) { throw new IllegalArgumentException("Unable to read image", e); }
        if (input.length > MAX_BYTES || input.length == 0)
            throw new IllegalArgumentException("Image too large");
        String mime;
        if (input.length > 8 && (input[0] & 255)==137 && input[1]==80 && input[2]==78 && input[3]==71)
            mime="image/png";
        else if(input.length > 3 && (input[0] & 255)==255 && (input[1] & 255)==216 && (input[2] & 255)==255)
            mime="image/jpeg";
        else throw new IllegalArgumentException("Only validated JPEG/PNG images supported in v1");
        BufferedImage original;
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
            Iterator<ImageReader> it=ImageIO.getImageReaders(stream);
            if(!it.hasNext()) throw new IllegalArgumentException("Unsupported image encoding");
            ImageReader reader=it.next();
            try {
                reader.setInput(stream,true,true);
                int w=reader.getWidth(0), h=reader.getHeight(0);
                if(w<1 || h<1 || w>4096 || h>4096 || (long)w*h>MAX_PIXELS)
                    throw new IllegalArgumentException("Image dimensions exceed limits");
                original=reader.read(0);
            } finally {reader.dispose();}
        } catch(IOException e) {throw new IllegalArgumentException("Invalid image",e);}
        if(original==null) throw new IllegalArgumentException("Invalid image");
        byte[] card=render(original,800,mime), thumb=render(original,192,mime);
        if (card.length>MAX_BYTES) throw new IllegalArgumentException("Normalized image too large");
        // Serialize quota checks for this workspace to avoid simultaneous uploads
        // independently observing the same available capacity.
        Integer locked = db.query("SELECT 1 FROM families WHERE id=? FOR UPDATE",
                rs -> rs.next() ? rs.getInt(1) : null, ws);
        if (locked == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Long used = db.queryForObject("""
                SELECT COALESCE(SUM(octet_length(b.bytes)),0) FROM media_blobs b
                JOIN media_assets a ON a.id=b.asset_id WHERE a.workspace_id=?
                """, Long.class, ws);
        if (used == null || used + card.length + thumb.length > 100L * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEDIA_WORKSPACE_QUOTA_EXCEEDED");
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO media_assets(id,workspace_id,mime_type,width,height,byte_size,sha256,created_by_member_id) VALUES (?::uuid,?,?,?,?,?,?,?)",
                id.toString(),ws,mime,original.getWidth(),original.getHeight(),card.length,hash(card),current.getCurrentMemberId());
        db.update("INSERT INTO media_blobs(asset_id,variant,bytes) VALUES (?::uuid,'CARD',?)",id.toString(),card);
        db.update("INSERT INTO media_blobs(asset_id,variant,bytes) VALUES (?::uuid,'THUMB',?)",id.toString(),thumb);
        return new Uploaded(id.toString(),mime,original.getWidth(),original.getHeight(),card.length);
    }
    private static byte[] render(BufferedImage source,int max,String mime) {
        double scale=Math.min(1, Math.min((double)max/source.getWidth(),(double)max/source.getHeight()));
        int w=Math.max(1,(int)Math.round(source.getWidth()*scale));
        int h=Math.max(1,(int)Math.round(source.getHeight()*scale));
        BufferedImage dst=new BufferedImage(w,h,mime.equals("image/png")?BufferedImage.TYPE_INT_ARGB:BufferedImage.TYPE_INT_RGB);
        Graphics2D g=dst.createGraphics();
        try {g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source,0,0,w,h,null);}finally {g.dispose();}
        try {ByteArrayOutputStream out=new ByteArrayOutputStream();
            if(!ImageIO.write(dst,mime.equals("image/png")?"png":"jpeg",out))
                throw new IllegalArgumentException("Image encoder unavailable");
            return out.toByteArray();}catch(IOException e){throw new IllegalArgumentException("Image encoding failed",e);}
    }
    private static String hash(byte[] bytes) {
        try {byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);}
        catch(Exception e){throw new IllegalStateException(e);}
    }

    @Transactional
    public Ref bind(long ws,String type,long target,String slot,String assetId,String catalogKey,Integer expectedVersion) {
        requireManager(ws,type,target);
        checkTarget(ws,type,target,slot);
        if ((assetId==null)==(catalogKey==null)) throw new IllegalArgumentException("Provide exactly one assetId or catalogKey");
        if(catalogKey!=null && !CATALOG.contains(catalogKey)) throw new IllegalArgumentException("Unknown catalog key");
        if(assetId!=null){
            UUID parsed=UUID.fromString(assetId);
            if(db.queryForObject("SELECT count(*) FROM media_assets WHERE id=?::uuid AND workspace_id=?",Integer.class,parsed.toString(),ws)!=1)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Media asset not found");
        }
        lockWorkspaceMedia(ws);
        Integer currentVersion=db.query("SELECT version FROM media_bindings WHERE workspace_id=? AND target_type=? AND target_id=? AND slot=? FOR UPDATE",
                rs->rs.next()?rs.getInt(1):null,ws,type,target,slot);
        if (expectedVersion == null || expectedVersion < 0)
            throw new IllegalArgumentException("expectedVersion is required");
        if (!Objects.equals(expectedVersion,currentVersion==null?0:currentVersion))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"MEDIA_VERSION_CONFLICT");
        int written=db.update("""
                INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,asset_id,catalog_key)
                VALUES (?,?,?,?,?::uuid,?)
                ON CONFLICT(workspace_id,target_type,target_id,slot)
                DO UPDATE SET asset_id=excluded.asset_id,catalog_key=excluded.catalog_key,
                version=media_bindings.version+1,updated_at=now()
                WHERE media_bindings.version=?
                """,ws,type,target,slot,assetId,catalogKey,expectedVersion);
        if (written != 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"MEDIA_VERSION_CONFLICT");
        return effective(ws,type,target,slot);
    }

    @Transactional
    public Ref clear(long ws,String type,long target,String slot,Integer expectedVersion) {
        requireManager(ws,type,target);
        checkTarget(ws,type,target,slot);
        // Preserve tombstone version after clearing to prevent the ABA anomaly.
        lockWorkspaceMedia(ws);
        Integer version=db.query("SELECT version FROM media_bindings WHERE workspace_id=? AND target_type=? AND target_id=? AND slot=? FOR UPDATE",
                rs -> rs.next() ? rs.getInt(1) : null,ws,type,target,slot);
        if (expectedVersion == null || expectedVersion < 0)
            throw new IllegalArgumentException("expectedVersion is required");
        if (!Objects.equals(expectedVersion,version==null?0:version))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"MEDIA_VERSION_CONFLICT");
        if (version != null)
            db.update("""
                UPDATE media_bindings SET asset_id=NULL,catalog_key=NULL,
                version=version+1,updated_at=now()
                WHERE workspace_id=? AND target_type=? AND target_id=? AND slot=?
                """,ws,type,target,slot);
        return effective(ws,type,target,slot);
    }

    @Transactional(readOnly=true)
    public Ref effective(long ws,String type,long target,String slot) {
        requireWorkspace(ws);checkTarget(ws,type,target,slot);
        var viewer=current.getCurrentMember();
        if (type.equals("MEMBER") && !viewer.getId().equals(target)
                && !viewer.hasPermission(WorkspacePermission.MANAGE_MEMBERS)
                && !viewer.hasPermission(WorkspacePermission.ADMIN_OVERRIDE))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media target not found");
        if (type.equals("MEMBER_GROUP") && !viewer.hasPermission(WorkspacePermission.MANAGE_MEMBERS)
                && !viewer.hasPermission(WorkspacePermission.ADMIN_OVERRIDE))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media target not found");
        if ((type.equals("WORK_NODE") || type.equals("TASK"))
                && !viewer.hasPermission(WorkspacePermission.VIEW_ALL_TASKS)
                && !viewer.hasPermission(WorkspacePermission.ADMIN_OVERRIDE))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media target not found");
        Ref own=getBinding(ws,type,target,slot,"OWN");
        if(own!=null)return own;
        if(type.equals("TASK")) {
            Long node=db.query("SELECT work_node_id FROM task_definitions WHERE id=? AND family_id=?",
                    rs->rs.next()?(Long)rs.getObject(1):null,target,ws);
            Set<Long> seen=new HashSet<>();
            for(int i=0;node!=null && i<32 && seen.add(node);i++) {
                Long parent=db.query("SELECT parent_node_id FROM work_nodes WHERE id=? AND family_id=?",
                        rs->rs.next()?(Long)rs.getObject(1):null,node,ws);
                if(parent==null)break;
                Ref parentImage=getBinding(ws,"WORK_NODE",parent,"ICON","INHERITED");
                if(parentImage!=null)return parentImage;
                node=parent;
            }
        }
        String key= switch(type) {
            case "WORKSPACE" -> slot.equals("COVER")?"family-cover-default":"family-default";
            case "MEMBER" -> "member-default";
            case "MEMBER_GROUP" -> "people-group-default";
            case "WORK_NODE" -> "task-group-default";
            default -> "task-default";
        };
        Integer bindingVersion=db.query("""
                SELECT version FROM media_bindings
                WHERE workspace_id=? AND target_type=? AND target_id=? AND slot=?
                """,rs -> rs.next() ? rs.getInt(1) : null,ws,type,target,slot);
        // The approved Soft Family dashboard hero is the built-in workspace
        // cover fallback. Clients must retain their existing static artwork.
        // Catalog preview art is a selectable override, not a replacement for it.
        return new Ref("DEFAULT",null,key,bindingVersion==null?0:bindingVersion,null);
    }
    private Ref getBinding(long ws,String type,long target,String slot,String source) {
        return db.query("SELECT asset_id::text,catalog_key,version FROM media_bindings WHERE workspace_id=? AND target_type=? AND target_id=? AND slot=? AND (asset_id IS NOT NULL OR catalog_key IS NOT NULL)",
                rs->rs.next()?new Ref(source,rs.getString(1),rs.getString(2),rs.getInt(3),
                        rs.getString(1)==null?null:"/workspaces/"+ws+"/media/"+rs.getString(1)+"/variants/"+
                                (type.equals("WORKSPACE") && slot.equals("COVER") ? "CARD" : "THUMB")):null,
                ws,type,target,slot);
    }

    @Transactional(readOnly=true)
    public ImageData getBoundVariant(long ws,String id,String variant) {
        requireWorkspace(ws);
        if(!Set.of("CARD","THUMB").contains(variant))throw new IllegalArgumentException("Invalid variant");
        UUID uuid=UUID.fromString(id);
        // A private asset can be read only if currently bound in this exact workspace.
        // Only expose a private image through a binding whose target the current
        // member is authorized to see. An opaque UUID is not authorization.
        var bindings=db.query("""
                SELECT b.target_type,b.target_id FROM media_bindings b
                JOIN media_assets a ON a.id=b.asset_id
                WHERE b.asset_id=?::uuid AND b.workspace_id=? AND a.workspace_id=?
                """,(rs,n)->new String[]{rs.getString(1),Long.toString(rs.getLong(2))},
                uuid.toString(),ws,ws);
        boolean visible=false;
        for (String[] binding:bindings) {
            try {
                String type=binding[0];long target=Long.parseLong(binding[1]);
                // Workspace bindings are authorized by target existence, without
                // guessing the slot. Other target types have only one v1 slot.
                checkTarget(ws,type,target,type.equals("MEMBER")?"AVATAR":"ICON");
                var member=current.getCurrentMember();
                if (type.equals("MEMBER") && member.getId()!=target
                        && !member.hasPermission(WorkspacePermission.MANAGE_MEMBERS)
                        && !member.hasPermission(WorkspacePermission.ADMIN_OVERRIDE)) continue;
                if ((type.equals("MEMBER_GROUP") || type.equals("WORK_NODE") || type.equals("TASK"))
                        && !member.hasPermission(type.equals("MEMBER_GROUP")
                            ? WorkspacePermission.MANAGE_MEMBERS : WorkspacePermission.VIEW_ALL_TASKS)
                        && !member.hasPermission(WorkspacePermission.ADMIN_OVERRIDE)) continue;
                visible=true;break;
            } catch(ResponseStatusException ignored) { /* revoked target */ }
        }
        if (!visible) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Media not found");
        try {
            return db.queryForObject("""
                    SELECT a.mime_type,v.bytes FROM media_assets a
                    JOIN media_blobs v ON a.id=v.asset_id WHERE a.id=?::uuid
                    AND a.workspace_id=? AND v.variant=?
                    """,(rs,n)->new ImageData(rs.getString(1),rs.getBytes(2)),uuid.toString(),ws,variant);
        }catch(EmptyResultDataAccessException e) {throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
    }

    private void lockWorkspaceMedia(long ws) {
        Integer locked=db.query("SELECT 1 FROM families WHERE id=? FOR UPDATE",
                rs -> rs.next() ? rs.getInt(1) : null, ws);
        if (locked == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    public void assertCatalogWorkspace(long ws) {requireWorkspace(ws);}
    // Stable global identifiers. A catalog entry never belongs to an individual
    // workspace and must not be counted against private media quotas.
    public record CatalogItem(String key, String category, String labelKey, int version,
                              boolean selectable, String previewPath) {}
    public List<CatalogItem> catalog() {
        return List.of(
            new CatalogItem("member-default", "AVATAR", "media.catalog.memberDefault", 1, true, "/workspaces/{workspaceId}/media/catalog/member-default/preview"),
            new CatalogItem("avatar-sunflower", "AVATAR", "media.catalog.avatarSunflower", 1, true, "/workspaces/{workspaceId}/media/catalog/avatar-sunflower/preview"),
            new CatalogItem("avatar-sky", "AVATAR", "media.catalog.avatarSky", 1, true, "/workspaces/{workspaceId}/media/catalog/avatar-sky/preview"),
            new CatalogItem("avatar-berry", "AVATAR", "media.catalog.avatarBerry", 1, true, "/workspaces/{workspaceId}/media/catalog/avatar-berry/preview"),
            new CatalogItem("avatar-mint", "AVATAR", "media.catalog.avatarMint", 1, true, "/workspaces/{workspaceId}/media/catalog/avatar-mint/preview"),
            new CatalogItem("avatar-peach", "AVATAR", "media.catalog.avatarPeach", 1, true, "/workspaces/{workspaceId}/media/catalog/avatar-peach/preview"),
            new CatalogItem("avatar-violet", "AVATAR", "media.catalog.avatarViolet", 1, true, "/workspaces/{workspaceId}/media/catalog/avatar-violet/preview"),
            new CatalogItem("avatar-ocean", "AVATAR", "media.catalog.avatarOcean", 1, true, "/workspaces/{workspaceId}/media/catalog/avatar-ocean/preview"),
            new CatalogItem("avatar-forest", "AVATAR", "media.catalog.avatarForest", 1, true, "/workspaces/{workspaceId}/media/catalog/avatar-forest/preview"),
            new CatalogItem("family-default", "FAMILY", "media.catalog.familyDefault", 1, true, "/workspaces/{workspaceId}/media/catalog/family-default/preview"),
            new CatalogItem("family-cover-default", "COVER", "media.catalog.familyCoverDefault", 1, true, "/workspaces/{workspaceId}/media/catalog/family-cover-default/preview"),
            new CatalogItem("people-group-default", "GROUP", "media.catalog.peopleGroupDefault", 1, true, "/workspaces/{workspaceId}/media/catalog/people-group-default/preview"),
            new CatalogItem("task-group-default", "TASK_CATEGORY", "media.catalog.taskGroupDefault", 1, true, "/workspaces/{workspaceId}/media/catalog/task-group-default/preview"),
            new CatalogItem("task-default", "TASK", "media.catalog.taskDefault", 1, true, "/workspaces/{workspaceId}/media/catalog/task-default/preview"),
            new CatalogItem("reward-star", "REWARD", "media.catalog.rewardStar", 1, true, "/workspaces/{workspaceId}/media/catalog/reward-star/preview")
        );
    }
    public byte[] catalogPreview(long workspaceId, String key) {
        requireWorkspace(workspaceId);
        if (!CATALOG.contains(key)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        try (InputStream in = getClass().getResourceAsStream("/media/catalog/" + key + ".svg")) {
            if (in == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            return in.readAllBytes();
        } catch (IOException e) { throw new IllegalStateException("Catalog asset unavailable", e); }
    }
}
