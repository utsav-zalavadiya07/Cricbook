import api from "@/services/api";

export type GovernanceMap = Record<string, any>;

export const getGovernanceFederations = (params?: { sport?: string; region?: string }) =>
  api.get<GovernanceMap[]>("/governance/federations", { params });

export const getGovernanceRankings = (params?: { sport?: string; type?: string }) =>
  api.get<GovernanceMap[]>("/governance/rankings", { params });
