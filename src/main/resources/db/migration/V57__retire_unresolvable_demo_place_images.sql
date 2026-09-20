UPDATE tourism_source.attraction_images
SET is_active = false,
    updated_at = now()
WHERE public_url LIKE 'https://daobk0bynum21.cloudfront.net/%';

UPDATE tourism_source.attractions
SET first_image1 = NULL
WHERE first_image1 LIKE 'https://daobk0bynum21.cloudfront.net/%';

UPDATE tourism_source.attractions
SET first_image2 = NULL
WHERE first_image2 LIKE 'https://daobk0bynum21.cloudfront.net/%';
