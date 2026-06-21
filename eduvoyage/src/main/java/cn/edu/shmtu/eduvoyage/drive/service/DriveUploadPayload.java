package cn.edu.shmtu.eduvoyage.drive.service;

import java.nio.file.Path;

record DriveUploadPayload(
        Path path,
        String name,
        String sha256,
        long size,
        String mime
) {
}
