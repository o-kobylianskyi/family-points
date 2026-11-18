package com.olehkobylianskyi.familypoints.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.util.*;

/**
 * Server-local encrypted .fpbackup artifact storage; no plaintext or keys.
 * The artifact is NOT verified for restore merely because storage succeeds.
 *
 * This component does not enable any reset/restore endpoint. Callers must
 * complete tenant authorization, archive authentication, restore rehearsal,
 * durable registry state and retention coordination independently.
 */
@Component
public final class WorkspaceBackupLocalArtifactStore {
    private static final int MAX_FILE_BYTES=64*1024*1024+32_000;
    private static final Set<PosixFilePermission> DIR_MODE=Set.of(
            PosixFilePermission.OWNER_READ,PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE);
    private static final Set<PosixFilePermission> FILE_MODE=Set.of(
            PosixFilePermission.OWNER_READ,PosixFilePermission.OWNER_WRITE);
    private final String configuredDirectory;
    private final ProtectiveBackupPortableFile format;

    public WorkspaceBackupLocalArtifactStore(
            @Value("${familypoints.backup.local-directory:}") String directory,
            ProtectiveBackupPortableFile format) {
        this.configuredDirectory=directory;
        this.format=format;
    }

    public record Stored(UUID snapshotId,long workspaceId,String sha256,int byteCount) {}

    public Stored store(long workspaceId,UUID snapshotId,byte[] encryptedFile) {
        if(workspaceId<=0 || snapshotId==null || encryptedFile==null
                || encryptedFile.length==0 || encryptedFile.length>MAX_FILE_BYTES)
            throw new IllegalArgumentException("INVALID_BACKUP_STORAGE_REQUEST");
        var artifact=format.decode(encryptedFile);
        if(artifact.manifest().workspaceId()!=workspaceId
                || !snapshotId.equals(artifact.manifest().snapshotId()))
            throw new SecurityException("BACKUP_STORAGE_SCOPE_MISMATCH");
        Path folder=workspaceFolder(workspaceId,true);
        Path destination=folder.resolve(snapshotId+".fpbackup");
        if(Files.exists(destination,LinkOption.NOFOLLOW_LINKS))
            throw new IllegalStateException("BACKUP_ARTIFACT_ALREADY_EXISTS");
        Path temp=null;
        boolean published=false;
        boolean completed=false;
        try {
            temp=Files.createTempFile(folder,".upload-",".tmp");
            chmod(temp,FILE_MODE);
            Files.write(temp,encryptedFile,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING);
            // Atomic publication; never overwrite a previously stored archive.
            // Same-directory hard-link publication is atomic and no-overwrite:
            // it cannot replace an existing snapshot from another writer.
            Files.createLink(destination,temp);
            published=true;
            Files.delete(temp);
            temp=null;
            chmod(destination,FILE_MODE);
            completed=true;
            return new Stored(snapshotId,workspaceId,ProtectiveBackupArtifactCodec.sha(encryptedFile),
                    encryptedFile.length);
        } catch(FileAlreadyExistsException ex) {
            throw new IllegalStateException("BACKUP_ARTIFACT_ALREADY_EXISTS",ex);
        } catch(IOException ex) {
            throw new IllegalStateException("BACKUP_LOCAL_WRITE_FAILED",ex);
        } finally {
            if(temp!=null)try {Files.deleteIfExists(temp);}catch(IOException ignored) {}
            // Remove only a destination published by THIS invocation. Never delete
            // a pre-existing snapshot after FileAlreadyExistsException.
            if(published && !completed)
                try {Files.deleteIfExists(destination);}catch(IOException ignored) {
                    // An orphan after cleanup failure is handled by audit/reconciliation.
                }
        }
    }

    public byte[] read(long workspaceId,UUID snapshotId,String expectedSha256) {
        if(workspaceId<=0 || snapshotId==null || expectedSha256==null
                || !expectedSha256.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("INVALID_BACKUP_READ_REQUEST");
        Path file=workspaceFolder(workspaceId,false).resolve(snapshotId+".fpbackup");
        try {
            if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)
                    || Files.size(file)>MAX_FILE_BYTES)
                throw new SecurityException("BACKUP_FILE_MISSING_OR_UNSAFE");
            byte[] contents=Files.readAllBytes(file);
            if(!MessageDigest.isEqual(
                    ProtectiveBackupArtifactCodec.sha(contents).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    expectedSha256.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
                Arrays.fill(contents,(byte)0);
                throw new SecurityException("BACKUP_STORAGE_HASH_MISMATCH");
            }
            var metadata=format.decode(contents);
            if(metadata.manifest().workspaceId()!=workspaceId
                    || !snapshotId.equals(metadata.manifest().snapshotId())) {
                Arrays.fill(contents,(byte)0);
                throw new SecurityException("BACKUP_STORAGE_SCOPE_MISMATCH");
            }
            return contents;
        } catch(IOException ex) {
            throw new IllegalStateException("BACKUP_LOCAL_READ_FAILED",ex);
        }
    }

    /**
     * Physically delete an EXACT snapshot after the registry/coordinator has
     * established expiry and no active restore lease. This method does not
     * independently decide which object is expired or safe to remove.
     */
    public boolean deleteAuthorizedExpired(long workspaceId,UUID snapshotId) {
        if(workspaceId<=0 || snapshotId==null)
            throw new IllegalArgumentException("INVALID_BACKUP_DELETE_REQUEST");
        Path file=workspaceFolder(workspaceId,false).resolve(snapshotId+".fpbackup");
        try {
            if(Files.isSymbolicLink(file))
                throw new SecurityException("BACKUP_SYMLINK_FORBIDDEN");
            return Files.deleteIfExists(file);
        } catch(IOException ex) {
            throw new IllegalStateException("BACKUP_LOCAL_DELETE_FAILED",ex);
        }
    }

    private Path workspaceFolder(long workspaceId,boolean create) {
        if(workspaceId<=0 || configuredDirectory==null || configuredDirectory.isBlank())
            throw new IllegalStateException("BACKUP_LOCAL_DIRECTORY_NOT_CONFIGURED");
        Path root=Path.of(configuredDirectory).toAbsolutePath().normalize();
        Path child=root.resolve(Long.toString(workspaceId));
        try {
            if(Files.isSymbolicLink(root) || Files.isSymbolicLink(child))
                throw new SecurityException("BACKUP_SYMLINK_FORBIDDEN");
            if(create) {
                Files.createDirectories(root);
                Files.createDirectories(child);
                chmod(root,DIR_MODE);
                chmod(child,DIR_MODE);
            }
            if(!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS)
                    || !Files.isDirectory(child,LinkOption.NOFOLLOW_LINKS))
                throw new IllegalStateException("BACKUP_LOCAL_FOLDER_MISSING");
            return child;
        } catch(IOException ex) {
            throw new IllegalStateException("BACKUP_LOCAL_FOLDER_FAILURE",ex);
        }
    }

    private static void chmod(Path path,Set<PosixFilePermission> perms)throws IOException {
        if(Files.getFileStore(path).supportsFileAttributeView("posix"))
            Files.setPosixFilePermissions(path,perms);
    }
}
