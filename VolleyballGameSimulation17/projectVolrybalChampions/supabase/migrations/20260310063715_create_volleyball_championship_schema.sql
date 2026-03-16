/*
  # Volleyball Championship Schema

  1. New Tables
    - `teams`
      - `id` (uuid, primary key)
      - `name` (text, unique)
      - `color` (text)
      - `wins` (integer, default 0)
      - `losses` (integer, default 0)
      - `created_at` (timestamptz)
    
    - `matches`
      - `id` (uuid, primary key)
      - `tournament_id` (uuid, references tournaments)
      - `team1_id` (uuid, references teams)
      - `team2_id` (uuid, references teams)
      - `team1_score` (integer, default 0)
      - `team2_score` (integer, default 0)
      - `winner_id` (uuid, references teams, nullable)
      - `round` (text) - 'semifinal' or 'final'
      - `match_number` (integer)
      - `status` (text, default 'pending') - 'pending', 'in_progress', 'completed'
      - `created_at` (timestamptz)
    
    - `tournaments`
      - `id` (uuid, primary key)
      - `name` (text)
      - `status` (text, default 'setup') - 'setup', 'in_progress', 'completed'
      - `created_at` (timestamptz)
  
  2. Security
    - Enable RLS on all tables
    - Add policies for public access (read and write)
*/

CREATE TABLE IF NOT EXISTS teams (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text UNIQUE NOT NULL,
  color text NOT NULL,
  wins integer DEFAULT 0,
  losses integer DEFAULT 0,
  created_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS tournaments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL,
  status text DEFAULT 'setup',
  created_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS matches (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tournament_id uuid REFERENCES tournaments(id) ON DELETE CASCADE,
  team1_id uuid REFERENCES teams(id),
  team2_id uuid REFERENCES teams(id),
  team1_score integer DEFAULT 0,
  team2_score integer DEFAULT 0,
  winner_id uuid REFERENCES teams(id),
  round text NOT NULL,
  match_number integer NOT NULL,
  status text DEFAULT 'pending',
  created_at timestamptz DEFAULT now()
);

ALTER TABLE teams ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournaments ENABLE ROW LEVEL SECURITY;
ALTER TABLE matches ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read access to teams"
  ON teams FOR SELECT
  TO anon
  USING (true);

CREATE POLICY "Allow public insert access to teams"
  ON teams FOR INSERT
  TO anon
  WITH CHECK (true);

CREATE POLICY "Allow public update access to teams"
  ON teams FOR UPDATE
  TO anon
  USING (true)
  WITH CHECK (true);

CREATE POLICY "Allow public delete access to teams"
  ON teams FOR DELETE
  TO anon
  USING (true);

CREATE POLICY "Allow public read access to tournaments"
  ON tournaments FOR SELECT
  TO anon
  USING (true);

CREATE POLICY "Allow public insert access to tournaments"
  ON tournaments FOR INSERT
  TO anon
  WITH CHECK (true);

CREATE POLICY "Allow public update access to tournaments"
  ON tournaments FOR UPDATE
  TO anon
  USING (true)
  WITH CHECK (true);

CREATE POLICY "Allow public delete access to tournaments"
  ON tournaments FOR DELETE
  TO anon
  USING (true);

CREATE POLICY "Allow public read access to matches"
  ON matches FOR SELECT
  TO anon
  USING (true);

CREATE POLICY "Allow public insert access to matches"
  ON matches FOR INSERT
  TO anon
  WITH CHECK (true);

CREATE POLICY "Allow public update access to matches"
  ON matches FOR UPDATE
  TO anon
  USING (true)
  WITH CHECK (true);

CREATE POLICY "Allow public delete access to matches"
  ON matches FOR DELETE
  TO anon
  USING (true);