package com.olehkobylianskyi.familypoints.service;

import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Versioned portable .fpbackup envelope for ALREADY AES-GCM-encrypted payloads.
 * No plaintext, passwords, raw keys, database credentials or SQL are contained
 * here. Decode is size-bounded; authenticity is verified by the AES-GCM codec,
 * never by this untrusted file parser alone.
 */
@Component
public final class ProtectiveBackupPortableFile {
    private static final int MAGIC=0x46504231; // FPB1
    private static final int VERSION=1;
    private static final int MAX_CIPHERTEXT=64*1024*1024+1024;
    private static final int MAX_STRING=4096;
    public byte[] encode(ProtectiveBackupArtifactCodec.EncryptedArtifact artifact) {
        try {
            var m=artifact.manifest();
            if(artifact.ciphertext()==null || artifact.ciphertext().length>MAX_CIPHERTEXT)
                throw new IllegalArgumentException("Backup payload exceeds limit");
            var bytes=new ByteArrayOutputStream();
            var out=new DataOutputStream(bytes);
            out.writeInt(MAGIC);out.writeInt(VERSION);
            write(out,m.snapshotId().toString());out.writeLong(m.workspaceId());
            write(out,m.operationId().toString());out.writeLong(m.dataEpoch());
            write(out,m.sourceDigest());write(out,m.schemaVersion());
            write(out,m.createdAt().toString());write(out,m.contentSha256());
            out.writeLong(m.contentBytes());
            out.writeInt(artifact.ciphertext().length);out.write(artifact.ciphertext());
            out.flush();return bytes.toByteArray();
        }catch(IOException e){throw new IllegalStateException("Could not encode portable backup",e);}
    }
    public ProtectiveBackupArtifactCodec.EncryptedArtifact decode(byte[] file) {
        if(file==null || file.length>MAX_CIPHERTEXT+MAX_STRING*6+512)
            throw new IllegalArgumentException("Invalid portable backup size");
        try {
            var in=new DataInputStream(new ByteArrayInputStream(file));
            if(in.readInt()!=MAGIC || in.readInt()!=VERSION)
                throw new IllegalArgumentException("Unsupported backup format");
            var snapshot=UUID.fromString(read(in));long workspace=in.readLong();
            var operation=UUID.fromString(read(in));long epoch=in.readLong();
            var digest=read(in);var schema=read(in);
            var created=Instant.parse(read(in));var hash=read(in);long size=in.readLong();
            int ciphertextLength=in.readInt();
            if(workspace<=0 || epoch<0 || size<0 || size>64L*1024*1024
                || !digest.matches("[0-9a-f]{64}") || !hash.matches("[0-9a-f]{64}")
                || schema.isBlank() || schema.length()>80
                || ciphertextLength<37 || ciphertextLength>MAX_CIPHERTEXT
                || ciphertextLength>in.available())
                throw new IllegalArgumentException("Invalid portable backup metadata");
            byte[] ciphertext=in.readNBytes(ciphertextLength);
            if(in.available()!=0)throw new IllegalArgumentException("Trailing portable backup bytes");
            var manifest=new ProtectiveBackupArtifactCodec.Manifest(snapshot,workspace,operation,
                epoch,digest,schema,created,hash,size);
            return new ProtectiveBackupArtifactCodec.EncryptedArtifact(manifest,ciphertext);
        }catch(IOException | java.time.format.DateTimeParseException e){
            throw new IllegalArgumentException("Malformed portable backup file",e);
        }
    }
    private static void write(DataOutputStream out,String value)throws IOException {
        byte[] data=value.getBytes(StandardCharsets.UTF_8);
        if(data.length>MAX_STRING)throw new IllegalArgumentException("Portable backup field too large");
        out.writeInt(data.length);out.write(data);
    }
    private static String read(DataInputStream in)throws IOException {
        int length=in.readInt();
        if(length<0 || length>MAX_STRING || length>in.available())
            throw new IllegalArgumentException("Invalid field length");
        return new String(in.readNBytes(length),StandardCharsets.UTF_8);
    }
}
