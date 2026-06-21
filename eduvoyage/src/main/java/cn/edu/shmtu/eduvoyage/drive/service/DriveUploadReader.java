package cn.edu.shmtu.eduvoyage.drive.service;

import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Streams multipart content to a temporary file while calculating sha256.
 */
@Component
class DriveUploadReader {

    Mono<DriveUploadPayload> read(FilePart file) {
        if (file == null) {
            return Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "上传文件不能为空"));
        }
        String name = DriveRules.normalizeName(DriveRules.clientFileName(file.filename()));
        BizException nameError = DriveRules.validateNodeName(name);
        if (nameError != null) {
            return Mono.error(nameError);
        }
        String mime = DriveRules.normalizeMime(file.headers().getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : file.headers().getContentType().toString());

        return Mono.fromCallable(() -> Files.createTempFile("eduvoyage-drive-", ".upload"))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(path -> writeAndDigest(file, path, name, mime)
                        .onErrorResume(e -> deleteTemp(path).then(Mono.error(e))));
    }

    private Mono<DriveUploadPayload> writeAndDigest(FilePart file, Path path, String name, String mime) {
        MessageDigest digest = sha256();
        AtomicLong size = new AtomicLong();
        return DataBufferUtils.write(file.content().doOnNext(buffer -> updateDigest(buffer, digest, size)), path)
                .then(Mono.fromCallable(() -> {
                    long bytes = size.get();
                    if (bytes <= 0) {
                        throw new BizException(BizErrorCode.PARAM_INVALID, "上传文件不能为空");
                    }
                    return new DriveUploadPayload(path, name, HexFormat.of().formatHex(digest.digest()), bytes, mime);
                }));
    }

    Mono<Void> deleteTemp(Path path) {
        if (path == null) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                        // best-effort temp cleanup
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static void updateDigest(DataBuffer buffer, MessageDigest digest, AtomicLong size) {
        try (DataBuffer.ByteBufferIterator it = buffer.readableByteBuffers()) {
            while (it.hasNext()) {
                ByteBuffer bytes = it.next();
                size.addAndGet(bytes.remaining());
                digest.update(bytes);
            }
        }
    }
}
