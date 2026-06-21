package com.soomgil.media.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MediaFileLookupMapper {

	@Select("""
		SELECT id, owner_user_id, storage_provider, bucket, object_key,
		       public_url, mime_type, byte_size, width, height,
		       linked_resource_type, linked_resource_id, status, created_at
		FROM media.media_files
		WHERE id = #{id} AND status = 'ACTIVE'
		""")
	Optional<MediaFileRecord> findById(@Param("id") UUID id);
}
