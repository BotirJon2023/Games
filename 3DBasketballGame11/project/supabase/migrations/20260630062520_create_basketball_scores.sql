CREATE TABLE IF NOT EXISTS basketball_scores (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  player_name TEXT NOT NULL,
  score INTEGER NOT NULL,
  level INTEGER NOT NULL,
  difficulty TEXT DEFAULT 'medium',
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable RLS
ALTER TABLE basketball_scores ENABLE ROW LEVEL SECURITY;

-- Create policies for public access (no auth required - public leaderboard)
CREATE POLICY "anyone_can_read_scores" ON basketball_scores FOR SELECT
  TO anon, authenticated USING (true);

CREATE POLICY "anyone_can_insert_scores" ON basketball_scores FOR INSERT
  TO anon, authenticated WITH CHECK (true);

-- Create index for leaderboard queries
CREATE INDEX idx_basketball_scores_score ON basketball_scores(score DESC);
CREATE INDEX idx_basketball_scores_level ON basketball_scores(level);
