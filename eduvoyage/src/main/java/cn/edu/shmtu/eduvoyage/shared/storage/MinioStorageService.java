package cn.edu.shmtu.eduvoyage.shared.storage;

import cn.edu.shmtu.eduvoyage.shared.config.EduVoyageProperties;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import io.minio.BucketExistsArgs;
import io.minio.ComposeObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SourceObject;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Reactive wrapper over {@link MinioAsyncClient}. All network operations return
 * {@code CompletableFuture}, bridged into Reactor via {@code Mono.fromFuture} so
 * the event-loop threads never block. The purely-local presigned-URL signing is
 * offloaded to {@code boundedElastic} as a defensive measure.
 *
 * <p>Chunked / resumable uploads are supported through the
 * "upload parts as temp objects, then {@link #composeObject} them" pattern:
 * the front end PUTs each slice to a temp key via a presigned URL, and the
 * drive module finalises by composing the parts into the destination object.</p>
 */
@Slf4j
@Service
public class MinioStorageService {

    private final MinioAsyncClient client;
    private final String bucket;
    private final int presignExpirySeconds;

    public MinioStorageService(MinioAsyncClient client, EduVoyageProperties properties) {
        this.client = client;
        this.bucket = properties.storage().minio().bucket();
        this.presignExpirySeconds = (int) properties.storage().minio().presignExpiry().toSeconds();
    }

    /**
     * Ensures the configured bucket exists at startup. Failures are logged but
     * do not abort boot (MinIO may come up slightly after the app in compose).
     */
    @PostConstruct
    void ensureBucket() {
        ensureBucketReady()
                .doOnError(e -> log.warn("MinIO bucket init failed for '{}': {}", bucket, e.toString()))
                .onErrorComplete()
                .subscribe(v -> {}, e -> {}, () -> log.info("MinIO bucket '{}' ready", bucket));
    }

    private Mono<Void> ensureBucketReady() {
        return Mono.defer(() -> {
                    try {
                        return Mono.fromFuture(client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()));
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                })
                .flatMap(exists -> exists
                        ? Mono.empty()
                        : Mono.defer(() -> {
                            try {
                                return Mono.fromFuture(client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())).then();
                            } catch (Exception e) {
                                return Mono.error(e);
                            }
                        }))
                .onErrorMap(e -> new BizException(BizErrorCode.STORAGE_ERROR, "存储桶初始化失败", e));
    }

    /** Uploads raw bytes to {@code objectKey}; resolves to the stored object's ETag. */
    public Mono<String> putBytes(String objectKey, byte[] content, String contentType) {
        return ensureBucketReady().then(Mono.defer(() -> {
                    try {
                        return Mono.fromFuture(client.putObject(PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectKey)
                                .stream(new ByteArrayInputStream(content), (long) content.length, -1L)
                                .contentType(contentType == null ? "application/octet-stream" : contentType)
                                .build()));
                    } catch (Exception e) {
                        return Mono.<io.minio.ObjectWriteResponse>error(
                                new BizException(BizErrorCode.STORAGE_ERROR, "上传失败: " + e.getMessage(), e));
                    }
                }))
                .map(resp -> resp.etag())
                .onErrorMap(e -> !(e instanceof BizException),
                        e -> new BizException(BizErrorCode.STORAGE_ERROR, "上传失败", e));
    }

    /** Uploads a local temporary file without loading it fully into heap memory. */
    public Mono<String> putFile(String objectKey, Path path, long size, String contentType) {
        return ensureBucketReady().then(Mono.defer(() -> {
                    InputStream input;
                    try {
                        input = Files.newInputStream(path);
                        CompletableFuture<io.minio.ObjectWriteResponse> future;
                        try {
                            future = client.putObject(PutObjectArgs.builder()
                                    .bucket(bucket)
                                    .object(objectKey)
                                    .stream(input, size, -1L)
                                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                                    .build());
                        } catch (Exception e) {
                            try {
                                input.close();
                            } catch (Exception ignored) {
                                // best-effort close
                            }
                            throw e;
                        }
                        future.whenComplete((resp, err) -> {
                            try {
                                input.close();
                            } catch (Exception ignored) {
                                // best-effort close
                            }
                        });
                        return Mono.fromFuture(future);
                    } catch (Exception e) {
                        return Mono.<io.minio.ObjectWriteResponse>error(
                                new BizException(BizErrorCode.STORAGE_ERROR, "上传失败: " + e.getMessage(), e));
                    }
                }))
                .map(resp -> resp.etag())
                .onErrorMap(e -> !(e instanceof BizException),
                        e -> new BizException(BizErrorCode.STORAGE_ERROR, "上传失败", e));
    }

    /**
     * Streams an object's bytes as a {@link Flux} without blocking the caller.
     * The blocking {@code InputStream} read is confined to {@code boundedElastic}.
     */
    public Flux<byte[]> getObjectStream(String objectKey, int chunkSize) {
        return Mono.defer(() -> {
                    try {
                        return Mono.fromFuture(client.getObject(
                                io.minio.GetObjectArgs.builder().bucket(bucket).object(objectKey).build()));
                    } catch (Exception e) {
                        return Mono.<io.minio.GetObjectResponse>error(
                                new BizException(BizErrorCode.FILE_NOT_FOUND, "文件不存在或读取失败", e));
                    }
                })
                .flatMapMany(response -> Flux.<byte[]>generate(sink -> {
                            try {
                                byte[] buf = response.readNBytes(chunkSize);
                                if (buf.length == 0) {
                                    sink.complete();
                                } else {
                                    sink.next(buf);
                                }
                            } catch (Exception e) {
                                sink.error(new BizException(BizErrorCode.STORAGE_ERROR, "读取失败", e));
                            }
                        })
                        .doFinally(sig -> {
                            try {
                                response.close();
                            } catch (Exception ignored) {
                                // best-effort close
                            }
                        }))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(e -> !(e instanceof BizException),
                        e -> new BizException(BizErrorCode.FILE_NOT_FOUND, "文件不存在或读取失败", e));
    }

    /** Generates a time-limited presigned GET URL for direct client download. */
    public Mono<String> presignedGetUrl(String objectKey) {
        return presignedUrl(objectKey, Http.Method.GET);
    }

    /** Generates a time-limited presigned PUT URL for direct client upload (incl. chunk parts). */
    public Mono<String> presignedPutUrl(String objectKey) {
        return presignedUrl(objectKey, Http.Method.PUT);
    }

    private Mono<String> presignedUrl(String objectKey, Http.Method method) {
        return Mono.fromCallable(() -> client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                        .method(method)
                        .bucket(bucket)
                        .object(objectKey)
                        .expiry(presignExpirySeconds, TimeUnit.SECONDS)
                        .build()))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(e -> new BizException(BizErrorCode.STORAGE_ERROR, "生成预签名URL失败", e));
    }

    /** Returns object metadata (size, etag, contentType); errors map to FILE_NOT_FOUND. */
    public Mono<StatObjectResponse> stat(String objectKey) {
        return Mono.defer(() -> {
                    try {
                        return Mono.fromFuture(client.statObject(StatObjectArgs.builder()
                                .bucket(bucket).object(objectKey).build()));
                    } catch (Exception e) {
                        return Mono.<StatObjectResponse>error(e);
                    }
                })
                .onErrorMap(e -> new BizException(BizErrorCode.FILE_NOT_FOUND, "文件不存在", e));
    }

    /** Removes a single object (idempotent from the caller's perspective). */
    public Mono<Void> remove(String objectKey) {
        return Mono.defer(() -> {
                    try {
                        return Mono.fromFuture(client.removeObject(RemoveObjectArgs.builder()
                                .bucket(bucket).object(objectKey).build()));
                    } catch (Exception e) {
                        return Mono.<Void>error(e);
                    }
                })
                .onErrorMap(e -> new BizException(BizErrorCode.STORAGE_ERROR, "删除失败", e));
    }

    /**
     * Composes previously-uploaded part objects into a single destination object
     * (server-side merge for resumable/chunked uploads). Resolves to the merged
     * object's ETag.
     */
    public Mono<String> composeObject(String destObjectKey, List<String> partObjectKeys) {
        List<SourceObject> sources = partObjectKeys.stream()
                .map(key -> SourceObject.builder().bucket(bucket).object(key).build())
                .toList();
        return Mono.defer(() -> {
                    try {
                        return Mono.fromFuture(client.composeObject(ComposeObjectArgs.builder()
                                .bucket(bucket)
                                .object(destObjectKey)
                                .sources(sources)
                                .build()));
                    } catch (Exception e) {
                        return Mono.<io.minio.ObjectWriteResponse>error(e);
                    }
                })
                .map(resp -> resp.etag())
                .onErrorMap(e -> new BizException(BizErrorCode.STORAGE_ERROR, "分片合并失败", e));
    }

    public String bucket() {
        return bucket;
    }
}
