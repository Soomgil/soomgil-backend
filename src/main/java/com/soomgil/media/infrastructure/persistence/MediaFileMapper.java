package com.soomgil.media.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MediaFileMapper {

	@Insert("""
		INSERT INTO media.media_files (
			id, owner_user_id, storage_provider, bucket, object_key,
			public_url, mime_type, byte_size, width, height,
			linked_resource_type, linked_resource_id, status, created_at
		) VALUES (
			#{id}, #{ownerUserId}, #{storageProvider}, #{bucket}, #{objectKey},
			#{publicUrl}, #{mimeType}, #{byteSize}, #{width}, #{height},
			#{linkedResourceType}, #{linkedResourceId}, #{status}, #{createdAt}
		)
		""")
	void insert(
		@Param("id") UUID id,
		@Param("ownerUserId") UUID ownerUserId,
		@Param("storageProvider") String storageProvider,
		@Param("bucket") String bucket,
		@Param("objectKey") String objectKey,
		@Param("publicUrl") String publicUrl,
		@Param("mimeType") String mimeType,
		@Param("byteSize") Long byteSize,
		@Param("width") Integer width,
		@Param("height") Integer height,
		@Param("linkedResourceType") String linkedResourceType,
		@Param("linkedResourceId") UUID linkedResourceId,
		@Param("status") String status,
		@Param("createdAt") Instant createdAt
	);

	@Select("""
		SELECT id, owner_user_id, storage_provider, bucket, object_key,
		       public_url, mime_type, byte_size, width, height,
		       linked_resource_type, linked_resource_id, status, created_at
		FROM media.media_files
		WHERE id = #{id} AND status = 'ACTIVE'
		""")
	Optional<MediaFileRecord> findById(@Param("id") UUID id);
}
