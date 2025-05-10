package lukas8219;

import com.automq.stream.*;
import com.automq.stream.api.CreateStreamOptions;
import com.automq.stream.s3.Config;
import com.automq.stream.s3.S3Storage;
import com.automq.stream.s3.S3StreamClient;
import com.automq.stream.s3.cache.blockcache.DefaultObjectReaderFactory;
import com.automq.stream.s3.cache.blockcache.StreamReaders;
import com.automq.stream.s3.failover.ForceCloseStorageFailureHandler;
import com.automq.stream.s3.failover.StorageFailureHandlerChain;
import com.automq.stream.s3.memory.MemoryMetadataManager;
import com.automq.stream.s3.operator.AwsObjectStorage;
import com.automq.stream.s3.wal.impl.object.ObjectReservationService;
import com.automq.stream.s3.wal.impl.object.ObjectWALConfig;
import com.automq.stream.s3.wal.impl.object.ObjectWALService;
import com.automq.stream.utils.SystemTime;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class Main {
    static Logger logger = Logger.getAnonymousLogger();

    public static void main(String[] s) throws ExecutionException, InterruptedException, IOException {
        var memoryManagers = new MemoryMetadataManager();
        var endpointUrl = Optional.ofNullable(System.getenv("MINIO_ENDPOINT_URL")).orElse("http://localhost:9000");
        var secretKey = Optional.ofNullable(System.getenv("MINIO_SECRET_KEY")).orElse("minioadmin");
        var accessKey = Optional.ofNullable(System.getenv("MINIO_ACCESS_KEY")).orElse("minioadmin");
        var bucketName = Optional.ofNullable(System.getenv("MINIO_BUCKET_NAME")).orElse("s3stream-test");

        logger.info("Using endpoint: " + endpointUrl);
        logger.info("Using secret key: " + secretKey);
        logger.info("Using access key: " + accessKey);
        logger.info("Using bucket name: " + bucketName);

        var basicCreds = AwsBasicCredentials
                .create(
                        accessKey,
                        secretKey
                );
        var objectStorage = new AwsObjectStorage(
                S3AsyncClient
                        .builder()
                        .endpointOverride(URI.create(endpointUrl))
                        .credentialsProvider(
                                StaticCredentialsProvider.create(basicCreds)

                        )
                        .region(
                                Region.US_EAST_2
                        )
                        .forcePathStyle(true)
                        .build(),
                bucketName
        );
        var config = new Config();
        var errorChain = new StorageFailureHandlerChain();
        var walReservationService = new ObjectReservationService(
               "test",
               objectStorage,
               objectStorage.bucketId()
        );
        walReservationService.acquire(0, 0, false).join();
        var wal = new ObjectWALService(
                new SystemTime(),
                objectStorage,
                ObjectWALConfig
                        .builder()
                        .withBucketId(objectStorage.bucketId())
                        .withClusterId("test")
                        .build()
        );
        wal.start();
        var storage = new S3Storage(
                config,
                wal,
                memoryManagers,
                memoryManagers,
                new StreamReaders(
                        100,
                        memoryManagers,
                        objectStorage,
                        new DefaultObjectReaderFactory(objectStorage)
                ),
                objectStorage,
                errorChain
        );
        logger.info("S3Storage started");
        var streamClient = new S3StreamClient(
                memoryManagers,
                storage,
                memoryManagers,
                objectStorage,
                config
        );
        errorChain.addHandler(new ForceCloseStorageFailureHandler(streamClient));

        var stream = streamClient.createAndOpenStream(
                CreateStreamOptions.builder().build()

        ).get();
        logger.info("Ok");

        var counter = new AtomicInteger();
        long maxDuration = System.currentTimeMillis() + 5000;
        while(System.currentTimeMillis() < maxDuration) {
            stream
                    .append(
                            new DefaultRecordBatch(
                                    1,
                                    System.nanoTime(),
                                    Map.of(),
                                    ByteBuffer.allocate(1)
                            )
                    )
                    .thenAccept((e) -> counter.incrementAndGet())
                    .exceptionally((e) -> {
                        logger.info(e.getMessage());
                        return null;
                    });
        }
        logger.info("Append was called " + counter);
        System.exit(0);
    }

}
