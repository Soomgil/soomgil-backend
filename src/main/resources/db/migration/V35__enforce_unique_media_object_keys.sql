CREATE UNIQUE INDEX media_files_bucket_object_key_unique
  ON media.media_files (bucket, object_key);
