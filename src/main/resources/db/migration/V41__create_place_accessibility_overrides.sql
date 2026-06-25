CREATE TABLE tourism_source.place_accessibility_overrides (
	provider varchar(40) NOT NULL,
	external_place_id varchar(120) NOT NULL,
	opening_hours text,
	closed_days text,
	parking_type varchar(20) NOT NULL DEFAULT 'UNKNOWN',
	flags_csv text NOT NULL DEFAULT '',
	unavailable_flags_csv text NOT NULL DEFAULT '',
	source_name varchar(120) NOT NULL,
	note text,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	updated_at timestamp with time zone NOT NULL DEFAULT now(),
	PRIMARY KEY (provider, external_place_id),
	CONSTRAINT chk_place_accessibility_overrides_parking_type
		CHECK (parking_type IN ('FREE', 'PAID', 'MIXED', 'NONE', 'UNKNOWN'))
);

CREATE INDEX idx_place_accessibility_overrides_source
	ON tourism_source.place_accessibility_overrides (source_name);

WITH seed (
	external_place_id,
	parking_type,
	flags_csv,
	unavailable_flags_csv,
	note
) AS (
	VALUES
		('11897234', 'PAID', 'WHEELCHAIR,DISABLED_TOILET,STROLLER', '', 'Jeju airport style demo place; terminal-scale accessibility seed.'),
		('228853', 'MIXED', 'WHEELCHAIR,STROLLER', '', 'Yongduam coastal viewpoint seed; accessible viewpoint paths may vary by approach.'),
		('1013246', 'PAID', 'WHEELCHAIR,STROLLER', '', 'Dongmun market seed; accessibility can vary by gate and crowding.'),
		('2660802', 'FREE', 'WHEELCHAIR,DISABLED_TOILET,STROLLER', '', 'Osulloc Tea Museum seed; indoor visitor facility.'),
		('126435', 'FREE', 'DISABLED_TOILET', 'WHEELCHAIR,STROLLER', 'Seongsan Ilchulbong trail/summit is stair and slope heavy.'),
		('598696', 'MIXED', 'STROLLER', 'WHEELCHAIR', 'Udo island movement depends on ferry/local mobility options.'),
		('126437', 'PAID', 'DISABLED_TOILET', 'WHEELCHAIR,STROLLER', 'Jeongbang waterfall access has stairs and uneven descent.'),
		('127053', 'PAID', 'WHEELCHAIR,DISABLED_TOILET,STROLLER', '', 'Jusangjeolli visitor deck seed.'),
		('127635', 'FREE', '', 'WHEELCHAIR,STROLLER', 'Hallasan hiking routes are trail based; not wheelchair suitable.'),
		('127490', 'MIXED', 'DISABLED_TOILET,STROLLER', 'WHEELCHAIR', 'Hyeopjae beach has accessible facilities but sand access is limited.'),
		('741109', 'FREE', 'WHEELCHAIR,DISABLED_TOILET,STROLLER', '', 'Camellia Hill garden seed; route conditions may vary by section.'),
		('27534592', 'PAID', 'WHEELCHAIR,STROLLER', '', 'Legacy demo id for Jeju Dongmun Market.'),
		('8239472', 'MIXED', 'WHEELCHAIR,STROLLER', '', 'Legacy demo id for Jeju cafe/gallery style stop.'),
		('78923471', 'FREE', 'DISABLED_TOILET', 'WHEELCHAIR,STROLLER', 'Legacy demo id for Seongsan Ilchulbong.'),
		('81234567', 'MIXED', 'STROLLER', 'WHEELCHAIR', 'Legacy demo id for Udo.'),
		('26748591', 'PAID', 'DISABLED_TOILET', 'WHEELCHAIR,STROLLER', 'Legacy demo id for Jeongbang waterfall.'),
		('89712346', 'PAID', 'WHEELCHAIR,DISABLED_TOILET,STROLLER', '', 'Legacy demo id for Jusangjeolli visitor deck.'),
		('24591827', 'FREE', '', 'WHEELCHAIR,STROLLER', 'Legacy demo id for Hallasan.'),
		('24252627', 'FREE', 'WHEELCHAIR,DISABLED_TOILET,STROLLER', '', 'Legacy demo id for Osulloc Tea Museum.'),
		('25262728', 'FREE', 'DISABLED_TOILET', 'WHEELCHAIR,STROLLER', 'Legacy demo id for Sanbangsan mountain area.'),
		('26272829', 'MIXED', 'DISABLED_TOILET,STROLLER', 'WHEELCHAIR', 'Legacy demo id for Jungmun beach.'),
		('36373839', 'MIXED', 'WHEELCHAIR,STROLLER', '', 'Legacy demo id for Hamdeok/cafe street style stop.'),
		('37383940', 'FREE', 'DISABLED_TOILET', 'WHEELCHAIR,STROLLER', 'Legacy demo id for cave/natural terrain style stop.'),
		('125445', 'UNKNOWN', 'STROLLER', 'WHEELCHAIR', 'Bulk Jeju KTO seed; terrain-heavy natural spot fallback.'),
		('126434', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126436', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126438', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126440', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126443', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126444', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126445', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126446', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126447', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126448', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126449', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126450', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126451', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126452', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126453', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.'),
		('126454', 'UNKNOWN', 'STROLLER', '', 'Bulk Jeju KTO seed; general attraction fallback.')
)
INSERT INTO tourism_source.place_accessibility_overrides (
	provider,
	external_place_id,
	parking_type,
	flags_csv,
	unavailable_flags_csv,
	source_name,
	note
)
SELECT
	'KTO',
	external_place_id,
	parking_type,
	flags_csv,
	unavailable_flags_csv,
	'jeju-accessibility-seed-v1',
	note
FROM seed
ON CONFLICT (provider, external_place_id) DO UPDATE SET
	parking_type = EXCLUDED.parking_type,
	flags_csv = EXCLUDED.flags_csv,
	unavailable_flags_csv = EXCLUDED.unavailable_flags_csv,
	source_name = EXCLUDED.source_name,
	note = EXCLUDED.note,
	updated_at = now();
