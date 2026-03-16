export interface Database {
  public: {
    Tables: {
      teams: {
        Row: {
          id: string;
          name: string;
          color: string;
          wins: number;
          losses: number;
          created_at: string;
        };
        Insert: {
          id?: string;
          name: string;
          color: string;
          wins?: number;
          losses?: number;
          created_at?: string;
        };
        Update: {
          id?: string;
          name?: string;
          color?: string;
          wins?: number;
          losses?: number;
          created_at?: string;
        };
      };
      tournaments: {
        Row: {
          id: string;
          name: string;
          status: string;
          created_at: string;
        };
        Insert: {
          id?: string;
          name: string;
          status?: string;
          created_at?: string;
        };
        Update: {
          id?: string;
          name?: string;
          status?: string;
          created_at?: string;
        };
      };
      matches: {
        Row: {
          id: string;
          tournament_id: string;
          team1_id: string;
          team2_id: string;
          team1_score: number;
          team2_score: number;
          winner_id: string | null;
          round: string;
          match_number: number;
          status: string;
          created_at: string;
        };
        Insert: {
          id?: string;
          tournament_id: string;
          team1_id: string;
          team2_id: string;
          team1_score?: number;
          team2_score?: number;
          winner_id?: string | null;
          round: string;
          match_number: number;
          status?: string;
          created_at?: string;
        };
        Update: {
          id?: string;
          tournament_id?: string;
          team1_id?: string;
          team2_id?: string;
          team1_score?: number;
          team2_score?: number;
          winner_id?: string | null;
          round?: string;
          match_number?: number;
          status?: string;
          created_at?: string;
        };
      };
    };
  };
}
