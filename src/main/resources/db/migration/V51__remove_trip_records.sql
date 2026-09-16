-- 기록 기능 폐지: 기록 전용 파일은 즉시 스토리지 삭제 대상으로 전환한다.
-- 실제 객체 삭제가 성공하기 전까지 키를 보존하여 기존 cleanup 작업이 재시도할 수 있게 한다.
-- 커뮤니티, 프로필 또는 다른 리소스에서 쓰는 공용 사진은 삭제하지 않는다.
UPDATE media.media_files mf
SET status = 'DELETED', deleted_at = COALESCE(deleted_at, now()), purge_after_at = now(),
    public_url = NULL
WHERE mf.status != 'PURGED'
  AND (
    mf.linked_resource_type = 'TRIP_RECORD'
    OR mf.object_key LIKE 'media/%/trip-record/%'
    OR EXISTS (SELECT 1 FROM record.trip_record_media rm WHERE rm.media_file_id = mf.id)
  )
  AND (mf.linked_resource_type IS NULL OR mf.linked_resource_type = 'TRIP_RECORD' OR mf.linked_resource_id IS NULL)
  AND NOT EXISTS (SELECT 1 FROM community.post_media pm WHERE pm.media_file_id = mf.id)
  AND NOT EXISTS (SELECT 1 FROM community.thread_media tm WHERE tm.media_file_id = mf.id)
  AND NOT EXISTS (SELECT 1 FROM community.posts p WHERE p.cover_media_file_id = mf.id)
  AND NOT EXISTS (SELECT 1 FROM auth.user_profiles up WHERE up.profile_media_file_id = mf.id);

UPDATE media.media_files
SET linked_resource_type = NULL, linked_resource_id = NULL
WHERE linked_resource_type = 'TRIP_RECORD';

-- 업로드 후 등록하지 않은 기록 파일도 기존 cleanup에서 즉시 정리한다.
UPDATE media.upload_intents
SET expires_at = now()
WHERE object_key LIKE 'media/%/trip-record/%' AND status IN ('PENDING', 'PURGING');

DROP TABLE record.trip_record_create_requests;
DROP TABLE record.trip_record_media;
DROP TABLE record.trip_record_entries;
DROP SCHEMA record;
