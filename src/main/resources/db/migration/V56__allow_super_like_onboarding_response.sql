ALTER TABLE preference.user_onboarding_responses
	DROP CONSTRAINT IF EXISTS chk_user_onboarding_response_reaction;

ALTER TABLE preference.user_onboarding_responses
	ADD CONSTRAINT chk_user_onboarding_response_reaction
		CHECK (reaction IN ('LIKE', 'NOPE', 'SUPER_LIKE'));
