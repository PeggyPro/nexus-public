/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.blobstore.s3.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * Uploads an InputStream, using multipart upload in parallel if the file is larger or equal to the chunk size.
 * A normal putObject request is used instead if only a single chunk would be sent.
 *
 * @since 3.next
 */
@Named("parallelUploader")
public class ParallelUploader
    extends StateGuardLifecycleSupport
    implements S3Uploader
{
  private final int chunkSize;

  private final int parallelism;

  private final ExecutorService executorService;

  @Inject
  public ParallelUploader(@Named("${nexus.s3.parallelupload.chunksize:-5242880}") final int chunkSize,
                          @Named("${nexus.s3.parallelupload.parallelism:-0}") final int nThreads)
  {
    checkArgument(nThreads >= 0, "Must use a non-negative parallelism");
    checkArgument(chunkSize >= 0, "Must use a non-negative chunkSize");
    this.chunkSize = chunkSize;
    this.parallelism = (nThreads > 0) ? nThreads : Runtime.getRuntime().availableProcessors();

    this.executorService = Executors.newFixedThreadPool(parallelism,
        new NexusThreadFactory("s3-parallel", "uploadThreads"));
  }

  @Override
  protected void doStop() {
    executorService.shutdownNow();
  }

  @Override
  public void upload(final AmazonS3 s3, final String bucket, final String key, final InputStream contents) {
    try (InputStream input = new BufferedInputStream(contents, chunkSize)) {
      input.mark(chunkSize);
      ChunkReader chunkReader = new ChunkReader(input);
      Optional<Chunk> firstChunk = chunkReader.readChunk(chunkSize);
      input.reset();

      if (!firstChunk.isPresent()) {
        log.error("Failed to read data for upload to key {} in bucket {}", key, bucket);
      }
      else if (firstChunk.get().dataLength < chunkSize) {
        try (InputStream arrayStream = new ByteArrayInputStream(firstChunk.get().data, 0,
            firstChunk.get().dataLength)) {
          uploadSinglePart(s3, bucket, key, arrayStream);
        }
      }
      else {
        log.debug("Starting multipart upload to key {} in bucket {}", key, bucket);
        InitiateMultipartUploadRequest initiateRequest = new InitiateMultipartUploadRequest(bucket, key);
        String uploadId = s3.initiateMultipartUpload(initiateRequest).getUploadId();
        uploadMultiPart(s3, bucket, key, input, uploadId);
        log.debug("Finished multipart upload id {} to key {} in bucket {}", uploadId, key, bucket);
      }
    }
    catch (IOException | SdkClientException e) { // NOSONAR
      throw new BlobStoreException(format("Error uploading blob to bucket:%s key:%s", bucket, key), e, null);
    }
  }

  private void uploadSinglePart(final AmazonS3 s3, final String bucket, final String key, final InputStream contents)
      throws IOException
  {
    log.debug("Starting upload of {} bytes to key {} in bucket {}", contents.available(), key, bucket);
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(contents.available());
    s3.putObject(bucket, key, contents, metadata);
  }

  private void uploadMultiPart(final AmazonS3 s3,
                               final String bucket,
                               final String key,
                               final InputStream inputStream,
                               final String uploadId)
  {
    try {
      CompletionService<List<PartETag>> completionService = new ExecutorCompletionService<>(executorService);
      ChunkReader chunkReader = new ChunkReader(inputStream);

      for (int i = 0; i < parallelism; i++) {
        completionService.submit(() -> uploadChunks(s3, bucket, key, uploadId, chunkReader));
      }

      List<PartETag> partETags = new ArrayList<>();
      for (int i = 0; i < parallelism; i++) {
        partETags.addAll(completionService.take().get());
      }

      s3.completeMultipartUpload(new CompleteMultipartUploadRequest()
          .withBucketName(bucket)
          .withKey(key)
          .withUploadId(uploadId)
          .withPartETags(partETags));
    }
    catch (CancellationException | ExecutionException | InterruptedException ex) {
      s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, key, uploadId));
      throw new BlobStoreException(
          format("Error uploading blob to bucket:%s key:%s for uploadId:%s", bucket, key, uploadId), ex, null);
    }
  }

  private List<PartETag> uploadChunks(final AmazonS3 s3,
                                      final String bucket,
                                      final String key,
                                      final String uploadId,
                                      final ChunkReader chunkReader)
      throws IOException
  {
    List<PartETag> tags = new ArrayList<>();
    Optional<Chunk> chunk;

    while ((chunk = chunkReader.readChunk(chunkSize)).isPresent()) {
      UploadPartRequest request = new UploadPartRequest()
          .withBucketName(bucket)
          .withKey(key)
          .withUploadId(uploadId)
          .withPartNumber(chunk.get().chunkNumber)
          .withInputStream(new ByteArrayInputStream(chunk.get().data, 0, chunk.get().dataLength))
          .withPartSize(chunk.get().dataLength);

      tags.add(s3.uploadPart(request).getPartETag());
    }

    return tags;
  }

  private static class ChunkReader
  {
    private final AtomicInteger counter;

    private final InputStream input;

    private ChunkReader(final InputStream input) {
      this.counter = new AtomicInteger(1);
      this.input = checkNotNull(input);
    }

    synchronized Optional<Chunk> readChunk(final int size) throws IOException
    {
      byte[] buf = new byte[size];
      int bytesRead = 0;
      int readSize;

      while ((readSize = input.read(buf, bytesRead, size - bytesRead)) != -1 && bytesRead < size) {
        bytesRead += readSize;
      }

      return bytesRead > 0 ? of(new Chunk(bytesRead, buf, counter.getAndIncrement())) : empty();
    }
  }

  private static class Chunk
  {
    final byte[] data;

    final int dataLength;

    final int chunkNumber;

    Chunk(final int dataLength, final byte[] data, final int chunkNumber) {
      this.dataLength = dataLength;
      this.data = data;  //NOSONAR
      this.chunkNumber = chunkNumber;
    }
  }
}