ALTER TABLE community.posts
  ADD COLUMN snapshot jsonb NOT NULL
  DEFAULT '{"days":[],"routes":[],"authorDisplay":null}'::jsonb;
