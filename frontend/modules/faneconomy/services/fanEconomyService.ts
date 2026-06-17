import api from "@/services/api";

export type FanEconomyMap = Record<string, any>;

export type FanProfilePayload = {
  displayName: string;
  email: string;
  city?: string;
  favoriteSport?: string;
  favoriteTeam?: string;
  favoritePlayer?: string;
  avatarUrl?: string;
};

export type FanClubPayload = {
  name: string;
  sport?: string;
  clubType?: string;
  linkedEntityType?: string;
  linkedEntityId?: number;
  city?: string;
  visibility?: string;
  description?: string;
};

export type CollectiblePayload = {
  title: string;
  collectibleType?: string;
  rarity?: string;
  sourceModule?: string;
  sourceRefId?: number;
  supplyLimit: number;
  mediaUrl?: string;
  credentialReference?: string;
};

export type PredictionGamePayload = {
  title: string;
  sport?: string;
  targetType?: string;
  targetRefId?: number;
  predictionType?: string;
  pointsForCorrect?: number;
  deadlineAt?: string;
  settlementRule?: string;
};

export type CreatorProfilePayload = {
  displayName: string;
  email: string;
  city?: string;
  creatorCategory?: string;
  bio?: string;
  profileUrl?: string;
};

export type CreatorContentPayload = {
  title: string;
  contentType?: string;
  sport?: string;
  mediaUrl?: string;
  visibility?: string;
  sponsored?: boolean;
};

export type MarketplaceListingPayload = {
  ownerFanProfileId: number;
  collectibleOwnershipId: number;
  price: number;
  currency?: string;
};

export type WatchPartyPayload = {
  title: string;
  hostFanProfileId: number;
  sport?: string;
  matchId?: number;
  tournamentId?: number;
  platform?: string;
  capacity?: number;
  scheduledAt?: string;
  accessMode?: string;
};

export const getFanEconomyDashboard = () => api.get<FanEconomyMap>("/fan-economy/dashboard");
export const getFanEconomyClubs = () => api.get<FanEconomyMap[]>("/fan-economy/clubs");
export const getFanEconomyCollectibles = () => api.get<FanEconomyMap[]>("/fan-economy/collectibles");
export const getFanEconomyPredictions = () => api.get<FanEconomyMap[]>("/fan-economy/predictions");
export const getFanEconomyCreators = () => api.get<FanEconomyMap[]>("/fan-economy/creators");
export const getFanEconomyMarketplaceListings = () => api.get<FanEconomyMap[]>("/fan-economy/marketplace/listings");
export const getFanEconomyWatchParties = () => api.get<FanEconomyMap[]>("/fan-economy/watch-parties");
export const createFanProfile = (payload: FanProfilePayload) => api.post<FanEconomyMap>("/fan-economy/fans", payload);
export const createFanClub = (payload: FanClubPayload) => api.post<FanEconomyMap>("/fan-economy/clubs", payload);
export const joinFanClub = (fanClubId: number, fanProfileId: number) => api.post<FanEconomyMap>(`/fan-economy/clubs/${fanClubId}/join`, { fanProfileId });
export const claimFanCollectible = (collectibleId: number, fanProfileId: number) => api.post<FanEconomyMap>(`/fan-economy/collectibles/${collectibleId}/claim`, { fanProfileId });
export const submitFanPrediction = (gameId: number, fanProfileId: number, predictionValue: string) =>
  api.post<FanEconomyMap>(`/fan-economy/predictions/${gameId}/entries`, { fanProfileId, predictionValue });
export const createCreatorProfile = (payload: CreatorProfilePayload) => api.post<FanEconomyMap>("/fan-economy/creators", payload);
export const publishCreatorContent = (creatorProfileId: number, payload: CreatorContentPayload) => api.post<FanEconomyMap>(`/fan-economy/creators/${creatorProfileId}/content`, payload);
export const createMarketplaceListing = (payload: MarketplaceListingPayload) => api.post<FanEconomyMap>("/fan-economy/marketplace/listings", payload);
export const purchaseMarketplaceListing = (listingId: number, buyerFanProfileId: number) => api.post<FanEconomyMap>(`/fan-economy/marketplace/listings/${listingId}/purchase`, { buyerFanProfileId });
export const createWatchParty = (payload: WatchPartyPayload) => api.post<FanEconomyMap>("/fan-economy/watch-parties", payload);
export const joinWatchParty = (watchPartyId: number, fanProfileId: number) => api.post<FanEconomyMap>(`/fan-economy/watch-parties/${watchPartyId}/join`, { fanProfileId });

export const getAdminFanEconomyDashboard = () => api.get<FanEconomyMap>("/admin/fan-economy/dashboard");
export const getAdminFanEconomyFans = () => api.get<FanEconomyMap[]>("/admin/fan-economy/fans");
export const createAdminFanCollectible = (payload: CollectiblePayload) => api.post<FanEconomyMap>("/admin/fan-economy/collectibles", payload);
export const createAdminPredictionGame = (payload: PredictionGamePayload) => api.post<FanEconomyMap>("/admin/fan-economy/predictions", payload);
export const scoreAdminPredictionEntry = (entryId: number, payload: { resultStatus: string; settlementNote?: string }) => api.post<FanEconomyMap>(`/admin/fan-economy/prediction-entries/${entryId}/score`, payload);
export const getAdminFanEconomyAudit = () => api.get<FanEconomyMap[]>("/admin/fan-economy/audit");
