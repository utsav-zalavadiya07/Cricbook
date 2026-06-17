import { apiRequest } from "../api/client";
import {
  Booking,
  AutoWaitlist,
  BookingIntelligence,
  BookingPrediction,
  BookingTemplate,
  ChatMessage,
  CommunityAnalytics,
  CommerceCart,
  CommerceOrder,
  CommerceProduct,
  Coupon,
  CouponValidation,
  FeedEvent,
  GamificationActivityType,
  GamificationAnalytics,
  GamificationLeaderboardEntry,
  GamificationProfile,
  LiveScore,
  LiveScoreDetail,
  MatchmakingAnalytics,
  MatchmakingMatch,
  NotificationAnnouncement,
  NotificationItem,
  NotificationSummary,
  OwnerInsights,
  OwnerDashboard,
  OwnerFinanceDashboard,
  OwnerSettings,
  PaymentRule,
  PaymentSplit,
  PersonalizationPayload,
  PlayerPerformance,
  PlayerPerformanceLeaderboardEntry,
  PlatformEpic,
  PlayerAvailability,
  PlayerConnection,
  PlayerDiscover,
  PlayerSettings,
  FrequentBooking,
  PublicMatch,
  Review,
  ResaleAnalytics,
  ResaleListing,
  ResaleRule,
  RecurringBooking,
  SearchFilters,
  SplitAnalytics,
  Slot,
  SlotAvailability,
  SlotWaitlist,
  Team,
  TeamAnalytics,
  TeamJoinRequest,
  TeamMatchStats,
  TeamRecruitment,
  TeamWorkspace,
  Tournament,
  TournamentFixture,
  TournamentStanding,
  TravelAccommodation,
  TravelEvent,
  TravelPackage,
  CareerMap,
  GovernanceMap,
  SuperAppMap,
  SportsOsMap,
  FanEconomyMap,
  SaasMap,
  CareerProfilePayload,
  Turf,
  UserProfile,
  Wallet,
  WeatherDashboard,
  WeatherPolicy,
} from "../types/models";

export const searchTurfs = (params: SearchFilters) => apiRequest<Turf[]>("/search", { query: params });
export const getTurfDetail = (turfId: number) => apiRequest<Turf>(`/turfs/${turfId}`);
export const getAvailability = (turfId: number, date: string) =>
  apiRequest<SlotAvailability[]>(`/turfs/${turfId}/availability`, { query: { date } });
export const getFavorites = () => apiRequest<Turf[]>("/favorites");
export const saveFavorite = (turfId: number) => apiRequest(`/favorites/${turfId}`, { method: "POST" });
export const removeFavorite = (turfId: number) => apiRequest(`/favorites/${turfId}`, { method: "DELETE" });
export const getFavoriteStatus = (turfId: number) => apiRequest<{ favorite?: boolean; isFavorite?: boolean }>(`/favorites/${turfId}`);
export const getPersonalization = () => apiRequest<PersonalizationPayload>("/personalization");
export const saveRecentSearch = (filters: SearchFilters) =>
  apiRequest("/personalization/recent-searches", {
    method: "POST",
    body: {
      city: filters.city,
      date: filters.date,
      boxType: filters.boxType,
      pitchType: filters.pitchType,
    },
  });

export const getMyBookings = () => apiRequest<Booking[]>("/bookings/my");
export const createBooking = (turfId: number, slotId: number, bookingDate: string) =>
  apiRequest<Booking>("/bookings", { method: "POST", body: { turfId, slotId, bookingDate } });
export const cancelBooking = (bookingId: number) => apiRequest(`/bookings/${bookingId}/cancel`, { method: "POST" });
export const rebookBooking = (bookingId: number, targetDate?: string) =>
  apiRequest<Booking>("/bookings/rebook", { method: "POST", body: { bookingId, targetDate } });
export const getFrequentBookings = () => apiRequest<FrequentBooking[]>("/bookings/frequent");
export const getBookingPredictions = () => apiRequest<BookingPrediction[]>("/bookings/predictions");
export const getRecurringBookings = () => apiRequest<RecurringBooking[]>("/bookings/recurring");
export const createRecurringBooking = (payload: { turfId: number; slotId: number; frequency: string; preferredDay?: string; nextBookingDate: string; autoConfirm?: boolean }) =>
  apiRequest<RecurringBooking>("/bookings/recurring", { method: "POST", body: payload });
export const getBookingTemplates = () => apiRequest<BookingTemplate[]>("/bookings/templates");
export const createBookingTemplate = (payload: { templateName: string; turfId: number; preferredDay?: string; preferredTime?: string; durationMinutes?: number; players?: number }) =>
  apiRequest<BookingTemplate>("/bookings/templates", { method: "POST", body: payload });
export const getAutoWaitlists = () => apiRequest<AutoWaitlist[]>("/bookings/auto-waitlist");
export const createAutoWaitlist = (payload: { slotId: number; preferredDate: string; autoBookEnabled?: boolean }) =>
  apiRequest<AutoWaitlist>("/bookings/auto-waitlist", { method: "POST", body: payload });
export const getBookingIntelligence = () => apiRequest<BookingIntelligence>("/bookings/intelligence");
export const getOwnerBookingIntelligence = () => apiRequest<BookingIntelligence>("/bookings/owner/intelligence");
export const getAdminBookingIntelligence = () => apiRequest<BookingIntelligence>("/bookings/admin/intelligence");
export const getWeatherDashboard = (city?: string) => apiRequest<WeatherDashboard>("/weather/dashboard", { query: { city } });
export const getOwnerWeatherDashboard = () => apiRequest<WeatherDashboard>("/weather/owner/dashboard");
export const getAdminWeatherDashboard = (city?: string) => apiRequest<WeatherDashboard>("/weather/admin/dashboard", { query: { city } });
export const getOwnerWeatherPolicy = () => apiRequest<WeatherPolicy>("/weather/owner/policy");
export const updateOwnerWeatherPolicy = (payload: Partial<WeatherPolicy>) =>
  apiRequest<WeatherPolicy>("/weather/owner/policy", { method: "PUT", body: payload });
export const getGamificationProfile = () => apiRequest<GamificationProfile>("/gamification/profile");
export const awardGamificationXp = (payload: { activityType: GamificationActivityType; points?: number; reason?: string }) =>
  apiRequest<GamificationProfile>("/gamification/xp", { method: "POST", body: payload });
export const claimGamificationReward = (rewardId: number) =>
  apiRequest(`/gamification/rewards/${rewardId}/claim`, { method: "POST" });
export const getGamificationLeaderboard = (city?: string) =>
  apiRequest<GamificationLeaderboardEntry[]>("/gamification/leaderboard", { query: { city } });
export const getOwnerGamificationAnalytics = () => apiRequest<GamificationAnalytics>("/gamification/owner/analytics");
export const getAdminGamificationAnalytics = () => apiRequest<GamificationAnalytics>("/gamification/admin/analytics");
export const getPlayerPerformance = () => apiRequest<PlayerPerformance>("/player-performance/me");
export const getPlayerPerformanceLeaderboard = (params?: { type?: "RUNS" | "WICKETS" | "MATCHES" | "AVERAGE" | "IMPACT"; city?: string; limit?: number }) =>
  apiRequest<PlayerPerformanceLeaderboardEntry[]>("/player-performance/leaderboard", { query: params });

export const getWallet = () => apiRequest<Wallet>("/wallet");
export const addMoney = (amount: number) => apiRequest("/wallet/add-money", { method: "POST", body: { amount } });
export const getCoupons = () => apiRequest<Coupon[]>("/coupons");
export const validateCoupon = (code: string, bookingAmount: number) =>
  apiRequest<CouponValidation>("/coupons/validate", { method: "POST", body: { code, bookingAmount } });
export const getGrowthDashboard = () => apiRequest<any>("/growth/dashboard");

export const getNotifications = (params?: { folder?: string; search?: string; channel?: string; type?: string; priority?: string; page?: number; size?: number }) =>
  apiRequest<NotificationItem[]>("/notifications", { query: params });
export const getNotificationSummary = () => apiRequest<NotificationSummary>("/notifications/summary");
export const getActiveAnnouncements = () => apiRequest<NotificationAnnouncement[]>("/notifications/announcements");
export const markNotificationRead = (id: number) => apiRequest(`/notifications/${id}/read`, { method: "PATCH" });
export const markAllNotificationsRead = () => apiRequest("/notifications/read-all", { method: "PATCH" });

export const getOpenTournaments = (params?: { query?: string; city?: string; format?: string; status?: string; maxEntryFee?: number }) =>
  apiRequest<Tournament[]>("/tournaments", { query: params });
export const getTournamentFixtures = (tournamentId: number) => apiRequest<TournamentFixture[]>(`/tournaments/${tournamentId}/fixtures`);
export const getTournamentStandings = (tournamentId: number) => apiRequest<TournamentStanding[]>(`/tournaments/${tournamentId}/standings`);
export const registerTeamForTournament = (tournamentId: number, teamId: number) =>
  apiRequest(`/tournaments/${tournamentId}/registrations`, { method: "POST", body: { teamId } });

export const getLiveScores = () => apiRequest<LiveScore[]>("/live-scores");
export const getLiveScore = (scoreId: number) => apiRequest<LiveScoreDetail>(`/live-scores/${scoreId}`);
export const createLiveScore = (payload: {
  fixtureId?: number;
  publicMatchId?: number;
  title?: string;
  teamAName: string;
  teamBName: string;
  battingTeam?: string;
  maxOvers: number;
}) => apiRequest<LiveScore>("/live-scores", { method: "POST", body: payload });

export const startMatch = (scoreId: number) =>
  apiRequest<LiveScore>(`/live-scores/${scoreId}/start`, { method: "POST" });

export const pauseMatch = (scoreId: number) =>
  apiRequest<LiveScore>(`/live-scores/${scoreId}/pause`, { method: "POST" });

export const resumeMatch = (scoreId: number) =>
  apiRequest<LiveScore>(`/live-scores/${scoreId}/resume`, { method: "POST" });

export const cancelMatch = (scoreId: number) =>
  apiRequest<LiveScore>(`/live-scores/${scoreId}/cancel`, { method: "POST" });

export const undoBall = (scoreId: number) =>
  apiRequest<LiveScoreDetail>(`/live-scores/${scoreId}/undo`, { method: "POST" });

export const editBall = (
  scoreId: number,
  eventId: number,
  payload: {
    runs: number;
    extraRuns: number;
    extraType?: string | null;
    wicket: boolean;
    wicketType?: string | null;
    batter: string;
    bowler: string;
    note?: string;
  }
) =>
  apiRequest<LiveScoreDetail>(`/live-scores/${scoreId}/events/${eventId}`, {
    method: "PUT",
    body: payload,
  });

export const deleteBall = (scoreId: number, eventId: number) =>
  apiRequest<LiveScoreDetail>(`/live-scores/${scoreId}/events/${eventId}`, {
    method: "DELETE",
  });

export const getMatchAnalytics = (scoreId: number) =>
  apiRequest<any>(`/live-scores/${scoreId}/analytics`);

export const overrideMatchResult = (scoreId: number, winnerTeamName: string) =>
  apiRequest<LiveScore>(`/live-scores/${scoreId}/override`, {
    method: "POST",
    query: { winnerTeamName },
  });

export const lockMatch = (scoreId: number) =>
  apiRequest<LiveScore>(`/live-scores/${scoreId}/lock`, { method: "POST" });

export const recordBall = (
  scoreId: number,
  payload: {
    runs: number;
    extraRuns: number;
    extraType?: string | null;
    wicket: boolean;
    wicketType?: string | null;
    batter: string;
    bowler: string;
    note?: string;
  }
) =>
  apiRequest<LiveScoreDetail>(`/live-scores/${scoreId}/events`, {
    method: "POST",
    body: payload,
  });

export const completeScore = (scoreId: number, payload?: number) =>
  apiRequest<LiveScore>(`/live-scores/${scoreId}/complete`, { method: "POST" });

export const getMyTeams = () => apiRequest<Team[]>("/social/teams");
export const createTeam = (payload: {
  name: string;
  sport: string;
  city: string;
  description?: string;
  logoUrl?: string;
  maxMembers: number;
  visibility?: string;
}) => apiRequest<Team>("/social/teams", { method: "POST", body: payload });
export const discoverTeams = (params?: { query?: string; city?: string; visibility?: string; openOnly?: boolean }) =>
  apiRequest<Team[]>("/social/teams/discover", { query: params });
export const requestToJoinTeam = (teamId: number, message?: string) =>
  apiRequest<TeamJoinRequest>(`/social/teams/${teamId}/join-requests`, { method: "POST", body: { message } });
export const getOpenMatches = () => apiRequest<PublicMatch[]>("/social/matches");
export const getMyMatchHistory = () => apiRequest<PublicMatch[]>("/social/matches/history");
export const createPublicMatch = (payload: {
  turfId?: number;
  title: string;
  sport: string;
  city: string;
  location: string;
  matchDate: string;
  startTime: string;
  endTime?: string;
  maxPlayers: number;
  visibility?: string;
  format?: string;
  overs?: number;
  entryFee?: number;
  homeTeamId?: number;
  awayTeamId?: number;
  challengeMessage?: string;
  notes?: string;
}) => apiRequest<PublicMatch>("/social/matches", { method: "POST", body: payload });
export const joinPublicMatch = (matchId: number) => apiRequest<PublicMatch>(`/social/matches/${matchId}/join`, { method: "POST" });
export const acceptMatchChallenge = (matchId: number) =>
  apiRequest<PublicMatch>(`/social/matches/${matchId}/accept`, { method: "POST" });
export const rejectMatchChallenge = (matchId: number) =>
  apiRequest<PublicMatch>(`/social/matches/${matchId}/reject`, { method: "POST" });
export const schedulePublicMatch = (matchId: number, payload: {
  turfId?: number;
  matchDate: string;
  startTime: string;
  endTime?: string;
  location?: string;
  note?: string;
}) => apiRequest<PublicMatch>(`/social/matches/${matchId}/schedule`, { method: "POST", body: payload });
export const startPublicMatch = (matchId: number) =>
  apiRequest<PublicMatch>(`/social/matches/${matchId}/start`, { method: "POST" });
export const cancelPublicMatch = (matchId: number) =>
  apiRequest<PublicMatch>(`/social/matches/${matchId}/cancel`, { method: "POST" });
export const submitMatchResult = (matchId: number, payload: {
  winnerTeamId?: number;
  homeScore: number;
  awayScore: number;
  resultMargin?: string;
  resultSummary?: string;
}) => apiRequest<PublicMatch>(`/social/matches/${matchId}/result`, { method: "POST", body: payload });
export const getTeamMatchStats = (teamId: number) =>
  apiRequest<TeamMatchStats>(`/social/teams/${teamId}/match-stats`);
export const discoverPlayers = (params?: { city?: string; position?: string; availability?: string }) =>
  apiRequest<PlayerDiscover[]>("/social/players/discover", { query: params });
export const getNearbyPlayers = (params?: { city?: string; availability?: string }) =>
  apiRequest<PlayerDiscover[]>("/social/players/nearby", { query: params });
export const getMyNetwork = () => apiRequest<PlayerConnection[]>("/social/network");
export const connectPlayer = (payload: { userId: number; type?: string; message?: string }) =>
  apiRequest<PlayerConnection>("/social/network/connect", { method: "POST", body: payload });
export const getOpenRecruitments = (params?: { city?: string; role?: string }) =>
  apiRequest<TeamRecruitment[]>("/social/recruitment/open", { query: params });
export const createRecruitment = (payload: { teamId: number; roleNeeded: string; skillLevel?: string; city?: string; availability?: string; description?: string; matchDate?: string }) =>
  apiRequest<TeamRecruitment>("/social/recruitment", { method: "POST", body: payload });
export const applyRecruitment = (recruitmentId: number, message?: string) =>
  apiRequest(`/social/recruitment/${recruitmentId}/apply`, { method: "POST", body: { message } });
export const getCommunityAnalytics = () => apiRequest<CommunityAnalytics>("/social/analytics");
export const getAdminCommunityModeration = () => apiRequest<CommunityAnalytics>("/social/admin/moderation");
export const getMatchRecommendations = () => apiRequest<MatchmakingMatch[]>("/matchmaking/recommendations");
export const getOpenMatchmaking = (params?: { city?: string; skillLevel?: string; trustedOnly?: boolean }) =>
  apiRequest<MatchmakingMatch[]>("/matchmaking/open", { query: params });
export const joinMatchmakingMatch = (matchId: number) =>
  apiRequest<MatchmakingMatch>(`/matchmaking/matches/${matchId}/join`, { method: "POST" });
export const getMatchmakingAnalytics = () => apiRequest<MatchmakingAnalytics>("/matchmaking/analytics");

export const getResaleListings = (city?: string) =>
  apiRequest<ResaleListing[]>("/resale/list", { query: city ? { city } : undefined });
export const getRecommendedResaleListings = () => apiRequest<ResaleListing[]>("/resale/recommended");
export const getMyResaleListings = () => apiRequest<ResaleListing[]>("/resale/my");
export const createResaleListing = (payload: { bookingId: number; price: number; expiresAt?: string; notes?: string }) =>
  apiRequest<ResaleListing>("/resale/create", { method: "POST", body: payload });
export const buyResaleListing = (listingId: number, paymentReference?: string) =>
  apiRequest<ResaleListing>(`/resale/${listingId}/buy`, { method: "POST", body: { paymentReference } });
export const joinResaleWaitlist = (payload: { slotId: number; date: string; city?: string; priority?: number }) =>
  apiRequest<SlotWaitlist>("/resale/waitlist", { method: "POST", body: payload });
export const getMyResaleWaitlist = () => apiRequest<SlotWaitlist[]>("/resale/waitlist/my");
export const getOwnerResaleAnalytics = () => apiRequest<ResaleAnalytics>("/resale/owner/analytics");
export const getAdminResaleAnalytics = () => apiRequest<ResaleAnalytics>("/resale/admin/analytics");
export const getOwnerResaleRules = () => apiRequest<ResaleRule>("/resale/owner/rules");
export const updateOwnerResaleRules = (payload: Partial<ResaleRule>) =>
  apiRequest<ResaleRule>("/resale/owner/rules", { method: "PUT", body: payload });

export const getCommerceProducts = (params?: { query?: string; sport?: string; category?: string }) =>
  apiRequest<CommerceProduct[]>("/commerce/products", { query: params });
export const getCommerceCart = () => apiRequest<CommerceCart>("/commerce/cart");
export const addCommerceCartItem = (payload: { productId: number; inventoryId: number; quantity: number; rentalStart?: string; rentalEnd?: string }) =>
  apiRequest<CommerceCart>("/commerce/cart", { method: "POST", body: payload });
export const checkoutCommerceCart = (payload: { shippingAddress?: string; couponCode?: string; loyaltyPoints?: number; paymentReference?: string }) =>
  apiRequest<CommerceOrder>("/commerce/checkout", { method: "POST", body: payload });
export const getTravelPackages = (params?: { query?: string; sport?: string; destination?: string }) =>
  apiRequest<TravelPackage[]>("/travel/packages", { query: params });
export const getTravelEvents = (params?: { eventType?: "EVENT" | "CAMP" | "EXPERIENCE" | "FESTIVAL"; query?: string; sport?: string; city?: string }) =>
  apiRequest<TravelEvent[]>("/travel/events", { query: params });
export const getTravelAccommodations = (params?: { city?: string; accommodationType?: string }) =>
  apiRequest<TravelAccommodation[]>("/travel/accommodations", { query: params });
export const bookTravelPackage = (packageId: number, payload: { bookingType?: string; participantCount?: number; travelerNames?: string; couponCode?: string; paymentReference?: string }) =>
  apiRequest(`/travel/packages/${packageId}/book`, { method: "POST", body: payload });
export const buyTravelTicket = (eventId: number, payload: { ticketTypeId: number; quantity?: number }) =>
  apiRequest(`/travel/events/${eventId}/tickets`, { method: "POST", body: payload });
export const bookTravelAccommodation = (accommodationId: number, payload: { checkIn: string; checkOut: string; rooms: number }) =>
  apiRequest(`/travel/accommodations/${accommodationId}/book`, { method: "POST", body: payload });
export const getCareerMarketplace = (params?: { city?: string; sport?: string; ageGroup?: string; minScore?: number }) =>
  apiRequest<CareerMap[]>("/career/marketplace", { query: params });
export const getCareerOpportunities = (params?: { type?: string; sport?: string; location?: string }) =>
  apiRequest<CareerMap[]>("/career/opportunities", { query: params });
export const createCareerProfile = (payload: CareerProfilePayload) =>
  apiRequest<CareerMap>("/career/profiles", { method: "POST", body: payload });
export const generateCareerResume = (profileId: number) =>
  apiRequest<CareerMap>(`/career/profiles/${profileId}/resume`, { method: "POST" });
export const addCareerAchievement = (profileId: number, payload: { title: string; achievementType?: string; issuer?: string; notes?: string }) =>
  apiRequest<CareerMap>(`/career/profiles/${profileId}/achievements`, { method: "POST", body: payload });
export const applyCareerOpportunity = (opportunityId: number, payload: { profileId: number; statement?: string; documentsUrl?: string }) =>
  apiRequest<CareerMap>(`/career/opportunities/${opportunityId}/applications`, { method: "POST", body: payload });
export const getGovernanceFederations = (params?: { sport?: string; region?: string }) =>
  apiRequest<GovernanceMap[]>("/governance/federations", { query: params });
export const getGovernanceRankings = (params?: { sport?: string; type?: string }) =>
  apiRequest<GovernanceMap[]>("/governance/rankings", { query: params });
export const getSuperAppDashboard = () => apiRequest<SuperAppMap>("/super-app/dashboard");
export const createSportsIdentity = (payload: {
  displayName: string;
  primaryEmail: string;
  phone?: string;
  city?: string;
  primarySport?: string;
  ageGroup?: string;
  guardianEmail?: string;
  profileVisibility?: string;
}) => apiRequest<SuperAppMap>("/super-app/identities", { method: "POST", body: payload });
export const generateSportsPassport = (identityId: number, payload?: { visibility?: string; expiresAt?: string }) =>
  apiRequest<SuperAppMap>(`/super-app/identities/${identityId}/passport`, { method: "POST", body: payload || {} });
export const creditUniversalWallet = (identityId: number, payload: { amount: number; sourceModule?: string; note?: string }) =>
  apiRequest<SuperAppMap>(`/super-app/identities/${identityId}/wallet/credit`, { method: "POST", body: payload });
export const earnUniversalRewards = (identityId: number, payload: { points: number; sourceModule?: string; note?: string }) =>
  apiRequest<SuperAppMap>(`/super-app/identities/${identityId}/rewards/earn`, { method: "POST", body: payload });
export const createUnifiedMembership = (identityId: number, payload: { tier: string; billingCycle?: string; price?: number; autoRenew?: boolean }) =>
  apiRequest<SuperAppMap>(`/super-app/identities/${identityId}/membership`, { method: "POST", body: payload });
export const addSportsTimelineEvent = (identityId: number, payload: { eventType: string; sourceModule?: string; title: string; visibility?: string }) =>
  apiRequest<SuperAppMap>(`/super-app/identities/${identityId}/timeline`, { method: "POST", body: payload });
export const getSportsOsDashboard = () => apiRequest<SportsOsMap>("/sports-os/dashboard");
export const searchSportsOsFacilities = (query?: string) => apiRequest<SportsOsMap[]>("/sports-os/facilities", { query: query ? { query } : undefined });
export const registerSportsVolunteer = (payload: {
  fullName: string;
  email: string;
  phone?: string;
  stateName: string;
  districtName: string;
  category?: string;
}) => apiRequest<SportsOsMap>("/sports-os/volunteers", { method: "POST", body: payload });
export const applyGovernmentGrant = (payload: {
  applicantName: string;
  applicantType?: string;
  programType?: string;
  stateName: string;
  districtName: string;
  requestedAmount: number;
}) => apiRequest<SportsOsMap>("/sports-os/grants", { method: "POST", body: payload });
export const getFanEconomyDashboard = () => apiRequest<FanEconomyMap>("/fan-economy/dashboard");
export const createFanEconomyProfile = (payload: {
  displayName: string;
  email: string;
  city?: string;
  favoriteSport?: string;
  favoriteTeam?: string;
  favoritePlayer?: string;
}) => apiRequest<FanEconomyMap>("/fan-economy/fans", { method: "POST", body: payload });
export const joinFanEconomyClub = (fanClubId: number, fanProfileId: number) =>
  apiRequest<FanEconomyMap>(`/fan-economy/clubs/${fanClubId}/join`, { method: "POST", body: { fanProfileId } });
export const claimFanEconomyCollectible = (collectibleId: number, fanProfileId: number) =>
  apiRequest<FanEconomyMap>(`/fan-economy/collectibles/${collectibleId}/claim`, { method: "POST", body: { fanProfileId } });
export const submitFanEconomyPrediction = (gameId: number, fanProfileId: number, predictionValue: string) =>
  apiRequest<FanEconomyMap>(`/fan-economy/predictions/${gameId}/entries`, { method: "POST", body: { fanProfileId, predictionValue } });
export const createFanEconomyWatchParty = (payload: {
  title: string;
  hostFanProfileId: number;
  sport?: string;
  capacity?: number;
  platform?: string;
  accessMode?: string;
}) => apiRequest<FanEconomyMap>("/fan-economy/watch-parties", { method: "POST", body: payload });
export const joinFanEconomyWatchParty = (watchPartyId: number, fanProfileId: number) =>
  apiRequest<FanEconomyMap>(`/fan-economy/watch-parties/${watchPartyId}/join`, { method: "POST", body: { fanProfileId } });
export const getSocialFeed = (params?: { scope?: string; query?: string; sort?: string; entityType?: string; entityId?: number }) =>
  apiRequest<FeedEvent[]>("/social/feed", { query: params });
export const createFeedPost = (payload: {
  title?: string;
  content: string;
  visibility?: "PUBLIC" | "FOLLOWERS" | "TEAM" | "PRIVATE";
  teamId?: number;
  tournamentId?: number;
  matchId?: number;
  mediaUrls?: string[];
}) => apiRequest<FeedEvent>("/social/feed", { method: "POST", body: payload });
export const editFeedPost = (postId: number, payload: {
  title?: string;
  content?: string;
  visibility?: "PUBLIC" | "FOLLOWERS" | "TEAM" | "PRIVATE";
  teamId?: number;
  tournamentId?: number;
  matchId?: number;
  mediaUrls?: string[];
}) => apiRequest<FeedEvent>(`/social/feed/${postId}`, { method: "PATCH", body: payload });
export const deleteFeedPost = (postId: number) =>
  apiRequest(`/social/feed/${postId}`, { method: "DELETE" });
export const reactToFeedPost = (postId: number, reactionType: "LIKE" | "AMAZING" | "GREAT_MATCH" | "WELL_PLAYED" = "LIKE") =>
  apiRequest<FeedEvent>(`/social/feed/${postId}/reactions`, { method: "POST", body: { reactionType } });
export const removeFeedReaction = (postId: number) =>
  apiRequest<FeedEvent>(`/social/feed/${postId}/reactions`, { method: "DELETE" });
export const getFeedComments = (postId: number) =>
  apiRequest<Array<{ id: number; feedEventId: number; authorId: number; authorName: string; parentCommentId?: number; message: string; status: string; editedAt?: string; createdAt: string }>>(
    `/social/feed/${postId}/comments`
  );
export const addFeedComment = (postId: number, message: string, parentCommentId?: number) =>
  apiRequest(`/social/feed/${postId}/comments`, { method: "POST", body: { message, parentCommentId } });
export const reportContent = (contentType: "POST" | "COMMENT" | "USER", contentId: number, reason: "SPAM" | "ABUSE" | "FAKE_CONTENT", details?: string) =>
  apiRequest("/social/reports", { method: "POST", body: { contentType, contentId, reason, details } });
export const getTeamMessages = (teamId: number) => apiRequest<ChatMessage[]>(`/social/chat/team/${teamId}`);
export const sendTeamMessage = (teamId: number, message: string, messageType: "TEXT" | "IMAGE" | "MATCH_INVITE" = "TEXT", mediaUrl?: string) =>
  apiRequest<ChatMessage>(`/social/chat/team/${teamId}`, { method: "POST", body: { message, messageType, mediaUrl } });
export const getMatchMessages = (matchId: number) => apiRequest<ChatMessage[]>(`/social/chat/match/${matchId}`);
export const sendMatchMessage = (matchId: number, message: string, messageType: "TEXT" | "IMAGE" | "MATCH_INVITE" = "TEXT", mediaUrl?: string) =>
  apiRequest<ChatMessage>(`/social/chat/match/${matchId}`, { method: "POST", body: { message, messageType, mediaUrl } });
export const getDirectMessages = (recipientId: number) =>
  apiRequest<ChatMessage[]>(`/social/chat/direct/${recipientId}`);
export const sendDirectMessage = (recipientId: number, message: string, messageType: "TEXT" | "IMAGE" | "MATCH_INVITE" = "TEXT", mediaUrl?: string) =>
  apiRequest<ChatMessage>(`/social/chat/direct/${recipientId}`, { method: "POST", body: { message, messageType, mediaUrl } });
export const getTeamWorkspace = (teamId: number) => apiRequest<TeamWorkspace>(`/team-workspaces/${teamId}`);
export const getTeamAnalytics = () => apiRequest<TeamAnalytics>("/team-workspaces/analytics");
export const getAdminTeamAnalytics = () => apiRequest<TeamAnalytics>("/team-workspaces/admin/analytics");
export const setTeamAvailability = (teamId: number, payload: { date: string; status: "AVAILABLE" | "MAYBE" | "UNAVAILABLE"; note?: string }) =>
  apiRequest(`/team-workspaces/${teamId}/availability`, { method: "POST", body: payload });
export const createTeamPoll = (teamId: number, payload: { question: string; options: string[] }) =>
  apiRequest(`/team-workspaces/${teamId}/polls`, { method: "POST", body: payload });
export const addTeamFinance = (teamId: number, payload: { type: "CONTRIBUTION" | "EXPENSE" | "REFUND"; amount: number; description: string }) =>
  apiRequest(`/team-workspaces/${teamId}/finances`, { method: "POST", body: payload });

export const getProfile = () => apiRequest<UserProfile>("/users/me");
export const updateProfile = (payload: { name?: string; email?: string; phone?: string }) =>
  apiRequest("/users/update", { method: "PUT", body: payload });
export const getPlayerSettings = () => apiRequest<PlayerSettings>("/settings");
export const updatePlayerSettings = (payload: Partial<PlayerSettings>) =>
  apiRequest<PlayerSettings>("/settings", { method: "PUT", body: payload });
export const requestPlayerDataExport = () => apiRequest<PlayerSettings>("/settings/data-export", { method: "POST" });
export const getUserBookings = () => apiRequest<Booking[]>("/users/bookings");
export const getUserReviews = () => apiRequest<Review[]>("/users/reviews");
export const getMyAvailability = () => apiRequest<PlayerAvailability>("/users/player-profile/availability");
export const updateMyAvailability = (payload: Partial<PlayerAvailability>) =>
  apiRequest<PlayerAvailability>("/users/player-profile/availability", { method: "PUT", body: payload });
export const getThemePreference = () => apiRequest<{ preferredTheme: string }>("/users/theme");
export const updateThemePreference = (preferredTheme: string) =>
  apiRequest<{ preferredTheme: string }>("/users/theme", { method: "PUT", body: { preferredTheme } });

export const getOwnerDashboard = () => apiRequest<OwnerDashboard>("/owner/dashboard");
export const getOwnerFinanceDashboard = () => apiRequest<OwnerFinanceDashboard>("/owner/finance/dashboard");
export const getOwnerSettings = () => apiRequest<OwnerSettings>("/owner/settings");
export const updateOwnerSettings = (payload: Partial<OwnerSettings>) =>
  apiRequest<OwnerSettings>("/owner/settings", { method: "PUT", body: payload });
export const requestOwnerDataExport = () => apiRequest<OwnerSettings>("/owner/settings/data-export", { method: "POST" });
export const getOwnerInsights = () => apiRequest<OwnerInsights>("/owner/insights");
export const getAdminOwnerInsights = () => apiRequest<OwnerInsights>("/admin/owner-insights");
export const addOwnerExpense = (payload: { type: string; amount: number; expenseDate?: string; note?: string }) =>
  apiRequest("/owner/insights/expenses", { method: "POST", body: payload });
export const askBusinessCoach = (question: string) =>
  apiRequest<OwnerInsights["coach"]>("/owner/insights/coach", { method: "POST", body: { question } });
export const getOwnerTurfs = () => apiRequest<Turf[]>("/turfs/my");
export const createTurf = (payload: {
  name: string;
  description?: string;
  city: string;
  location: string;
  latitude?: string;
  longitude?: string;
  pricePerHour: string;
  pitchType: string;
  boxType: string;
  amenities?: string;
}) => {
  const form = new FormData();
  form.append("name", payload.name);
  form.append("description", payload.description || "");
  form.append("city", payload.city);
  form.append("location", payload.location);
  form.append("latitude", payload.latitude || "");
  form.append("longitude", payload.longitude || "");
  form.append("pricePerHour", payload.pricePerHour);
  form.append("pitchType", payload.pitchType);
  form.append("boxType", payload.boxType);
  (payload.amenities || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .forEach((amenity) => form.append("amenities", amenity));
  return apiRequest("/turfs", { method: "POST", body: form });
};
export const setTurfStatus = (id: number, status: "activate" | "deactivate" | "maintenance") =>
  apiRequest(`/turfs/${id}/${status}`, { method: "PUT" });
export const getTurfSlots = (turfId: number) => apiRequest<Slot[]>(`/turfs/${turfId}/slots`);
export const createSlots = (turfId: number, payload: { startTime: string; endTime: string; slotDuration: number; price: number }) =>
  apiRequest(`/turfs/${turfId}/slots`, { method: "POST", body: payload });
export const updateSlot = (turfId: number, slotId: number, payload: { startTime?: string; endTime?: string; price?: number; isActive?: boolean }) =>
  apiRequest(`/turfs/${turfId}/slots/${slotId}`, { method: "PUT", body: payload });
export const disableSlot = (turfId: number, slotId: number) =>
  apiRequest(`/turfs/${turfId}/slots/${slotId}/disable`, { method: "POST" });
export const applySlotPricing = (turfId: number, payload: { slotIds: number[]; date: string; price: number; reason?: string }) =>
  apiRequest(`/turfs/${turfId}/slots/pricing`, { method: "POST", body: payload });
export const blockSlots = (turfId: number, payload: { date: string; slotIds: number[]; blocked?: boolean }) =>
  apiRequest(`/turfs/${turfId}/block`, { method: "POST", body: payload });
export const unblockSlots = (turfId: number, payload: { date: string; slotIds: number[] }) =>
  apiRequest(`/turfs/${turfId}/block`, { method: "POST", body: { ...payload, blocked: false } });

export const getOwnerBookings = (status?: string) =>
  apiRequest<Booking[]>("/bookings/owner", { query: status ? { status } : undefined });
export const cancelOwnerBooking = (bookingId: number) =>
  apiRequest(`/bookings/owner/${bookingId}/cancel`, { method: "POST" });
export const checkInOwnerBooking = (bookingId: number) =>
  apiRequest<Booking>(`/bookings/owner/${bookingId}/check-in`, { method: "POST", body: { note: "Checked in from mobile owner workspace." } });
export const completeOwnerBooking = (bookingId: number) =>
  apiRequest<Booking>(`/bookings/owner/${bookingId}/complete`, { method: "POST", body: { note: "Completed from mobile owner workspace." } });
export const markOwnerNoShow = (bookingId: number) =>
  apiRequest<Booking>(`/bookings/owner/${bookingId}/no-show`, { method: "POST", body: { reason: "No-show recorded from mobile owner workspace." } });
export const triggerRefund = (bookingId: number) =>
  apiRequest(`/payments/refund/${bookingId}`, { method: "POST" });
export const getOwnerReviews = () => apiRequest<Review[]>("/reviews/owner");
export const replyToReview = (reviewId: number, reply: string) =>
  apiRequest<Review>(`/reviews/${reviewId}/owner-reply`, { method: "POST", body: { reply } });
export const createSplitPayment = (payload: { bookingId: number; splitType?: string; dueDate?: string; shares: Array<{ userId?: number; loginId?: string; amount?: number }> }) =>
  apiRequest("/payment/split", { method: "POST", body: payload });
export const getMySplitPayments = () => apiRequest<PaymentSplit[]>("/payment/splits/my");
export const paySplitShare = (payload: { splitId: number; paymentReference?: string; useWallet?: boolean }) =>
  apiRequest("/payment/pay-share", { method: "POST", body: payload });
export const getSplitAnalytics = () => apiRequest<SplitAnalytics>("/payment/analytics");
export const getAdminPaymentMonitoring = () => apiRequest<SplitAnalytics>("/admin/payment-monitoring");
export const getOwnerPaymentRules = () => apiRequest<PaymentRule>("/owner/payment-rules");
export const updateOwnerPaymentRules = (payload: Partial<PaymentRule>) =>
  apiRequest<PaymentRule>("/owner/payment-rules", { method: "PUT", body: payload });

// === EPIC 16 ADDITIONS: MEMBERSHIP PLANS, SMART PLATFORM, AND SUPER ADMIN API LAYER ===

// 1. Membership Services
export const getMembershipPlans = () => apiRequest<any[]>("/memberships/plans");
export const getMyMemberships = () => apiRequest<any[]>("/memberships/my");
export const subscribeMembership = (planId: number, turfId?: number) =>
  apiRequest("/memberships/subscribe", { method: "POST", body: { planId, turfId } });

// 2. Smart Platform Recommendations
export const getSmartRecommendations = (params?: { city?: string; turfId?: number }) =>
  apiRequest<any>("/smart/recommendations", { query: params });

// 3. Super Admin Operations Console
export const getAdminAnalytics = () => apiRequest<any>("/admin/analytics");
export const getAdminBIWorkspace = () => apiRequest<any>("/admin/analytics/bi");
export const getAdminBIReport = (type = "REVENUE", period = "MONTHLY") =>
  apiRequest<any>("/admin/analytics/reports", { query: { type, period } });
export const getAdminGrowthDashboard = () => apiRequest<any>("/admin/growth/dashboard");
export const getAdminGrowthAutomation = () => apiRequest<any[]>("/admin/growth/automation");
export const getAdminOwners = () => apiRequest<any[]>("/admin/owners");
export const updateAdminUserStatus = (userId: number, status: "ACTIVE" | "BLOCKED" | "UNVERIFIED") =>
  apiRequest(`/admin/users/${userId}/status`, { method: "PATCH", body: { status } });
export const getAdminTurfs = () => apiRequest<Turf[]>("/admin/turfs");
export const updateAdminTurfStatus = (turfId: number, status: "ACTIVE" | "INACTIVE" | "MAINTENANCE") =>
  apiRequest(`/admin/turfs/${turfId}/status`, { method: "PATCH", body: { status } });
export const getAdminReviews = () => apiRequest<any[]>("/admin/reviews");
export const deleteAdminReview = (reviewId: number) =>
  apiRequest(`/admin/reviews/${reviewId}`, { method: "DELETE" });
export const getCampaigns = () => apiRequest<any[]>("/campaigns");
export const createCampaign = (payload: {
  name: string;
  title: string;
  message: string;
  channel: "EMAIL" | "PUSH" | "SMS" | "WHATSAPP";
  targetRole?: string;
  deepLink?: string;
}) => apiRequest("/campaigns", { method: "POST", body: payload });
export const sendCampaign = (campaignId: number) =>
  apiRequest(`/campaigns/${campaignId}/send`, { method: "POST" });

// === EPIC 17 OPERATIONAL ENRICHMENTS ===

export const getAdminOpsUsers = () => apiRequest<any[]>("/admin/ops/users");

export const warnUser = (userId: number, reason: string) =>
  apiRequest(`/admin/ops/users/${userId}/warn`, { method: "POST", body: { reason } });

export const banUser = (userId: number, reason: string) =>
  apiRequest(`/admin/ops/users/${userId}/ban`, { method: "POST", body: { reason } });

export const unbanUser = (userId: number) =>
  apiRequest(`/admin/ops/users/${userId}/unban`, { method: "POST" });

export const getOwnerVerifications = () => apiRequest<any[]>("/admin/ops/owner-verifications");

export const approveOwner = (id: number) =>
  apiRequest(`/admin/ops/owner-verifications/${id}/approve`, { method: "POST" });

export const rejectOwner = (id: number) =>
  apiRequest(`/admin/ops/owner-verifications/${id}/reject`, { method: "POST" });

export const getTurfModerations = () => apiRequest<any[]>("/admin/ops/turf-moderations");

export const approveTurf = (id: number) =>
  apiRequest(`/admin/ops/turf-moderations/${id}/approve`, { method: "POST" });

export const rejectTurf = (id: number, reason: string) =>
  apiRequest(`/admin/ops/turf-moderations/${id}/reject`, { method: "POST", body: { reason } });

export const getOpsBookings = () => apiRequest<any[]>("/admin/ops/bookings");

export const overrideBookingStatus = (bookingId: number, status: string) =>
  apiRequest(`/admin/ops/bookings/${bookingId}/override`, { method: "POST", body: { status } });

export const triggerManualRefund = (bookingId: number) =>
  apiRequest(`/admin/ops/bookings/${bookingId}/refund`, { method: "POST" });

export const getOpsFinance = () => apiRequest<any>("/admin/ops/finance");

export const getOpsSettings = () => apiRequest<any[]>("/admin/ops/settings");

export const saveOpsSetting = (key: string, value: string) =>
  apiRequest("/admin/ops/settings", { method: "POST", body: { key, value } });

export const getFraudAlerts = () => apiRequest<any[]>("/admin/ops/fraud-alerts");

export const dismissFraudAlert = (id: number) =>
  apiRequest(`/admin/ops/fraud-alerts/${id}/dismiss`, { method: "POST" });

export const getOpsCmsContent = () => apiRequest<any[]>("/admin/ops/cms");

export const addOpsCmsContent = (payload: { type: string; title: string; imageUrl?: string; status?: string }) =>
  apiRequest("/admin/ops/cms", { method: "POST", body: payload });

export const deleteOpsCmsContent = (id: number) =>
  apiRequest(`/admin/ops/cms/${id}`, { method: "DELETE" });

export const getOpsAuditLogs = () => apiRequest<any[]>("/admin/ops/audit-logs");

export const getAdminGovernanceDashboard = () => apiRequest<any>("/admin/ops/governance/dashboard");

export const getAdminRbacMatrix = () => apiRequest<any>("/admin/ops/rbac/matrix");

export const getSupportTickets = () => apiRequest<any[]>("/admin/ops/support-tickets");

export const replySupportTicket = (ticketId: number, payload: { reply: string; status: string; internalNotes?: string }) =>
  apiRequest(`/admin/ops/support-tickets/${ticketId}/reply`, { method: "POST", body: payload });

export const getIncidents = () => apiRequest<any[]>("/admin/ops/incidents");

export const createIncident = (payload: { name: string; status?: string; severity: string; message: string }) =>
  apiRequest("/admin/ops/incidents", { method: "POST", body: payload });

export const resolveIncident = (id: number) =>
  apiRequest(`/admin/ops/incidents/${id}/resolve`, { method: "POST" });

// === EPIC 22 AI ASSISTANT & PLATFORM INTELLIGENCE ===
export const getAIRecommendations = (category?: string) =>
  apiRequest<any>("/ai/recommendations", { query: category ? { category } : undefined });

export const smartAISearch = (query: string, city?: string) =>
  apiRequest<any[]>("/ai/search/smart", { query: { query, city } });

export const chatAIAssistant = (message: string, conversationId?: string) =>
  apiRequest<any>("/ai/assistant/chat", { method: "POST", body: { message, conversationId } });

export const getAIIntelligence = () => apiRequest<any>("/ai/intelligence");

export const getAIRevenueForecasts = () => apiRequest<any[]>("/ai/forecasts/revenue");

export const sendAIRecommendationFeedback = (payload: { targetType: string; targetId?: number; feedbackType: "LIKE" | "DISLIKE" | "IGNORE"; reason?: string }) =>
  apiRequest<any>("/ai/recommendations/feedback", { method: "POST", body: payload });

export const getAIPricingRules = (turfId: number) =>
  apiRequest<any[]>(`/owner/pricing/rules`, { query: { turfId } });

export const toggleAIPricingRule = (payload: { turfId: number; ruleName: string; active: boolean }) =>
  apiRequest("/owner/pricing/toggle", { method: "POST", body: payload });

export const getAIPricingHistory = (turfId: number) =>
  apiRequest<any[]>(`/owner/pricing/history`, { query: { turfId } });

export const getAIFraudAlerts = () => apiRequest<any[]>("/admin/ai/fraud");
export const getAICheckExperiments = () => apiRequest<any[]>("/admin/ai/experiments");
export const getAIChurnScores = () => apiRequest<any[]>("/admin/ai/churn");
export const getAdminAIDashboard = () => apiRequest<any>("/admin/ai/dashboard");
export const getAdminAIGovernance = () => apiRequest<any>("/admin/ai/governance");
export const askAdminAICopilot = (query: string) =>
  apiRequest<any>("/admin/ai/copilot/query", { method: "POST", body: { query } });

// === EPIC 22 GLOBAL SPORTS PLATFORM API LAYER ===
export const getExpansionConfigs = () => apiRequest<any[]>("/global/configs");
export const getTranslations = (lang: string) => apiRequest<Record<string, string>>("/global/translations", { query: { lang } });
export const getExchangeRates = () => apiRequest<any[]>("/global/exchange-rates");
export const getExpansionAnalytics = () => apiRequest<any>("/admin/expansion/analytics");
export const getAdminEnterpriseDashboard = () => apiRequest<any>("/admin/enterprise/dashboard");
export const getAdminEnterpriseCenters = () => apiRequest<any[]>("/admin/enterprise/centers");
export const getAdminEnterpriseStaff = () => apiRequest<any[]>("/admin/enterprise/staff");
export const getAdminEnterpriseFranchises = () => apiRequest<any[]>("/admin/enterprise/franchises");
export const getAdminEnterpriseReports = () => apiRequest<any>("/admin/enterprise/reports");
export const getAdminEnterpriseGovernance = () => apiRequest<any>("/admin/enterprise/governance");
export const saveBranding = (payload: { tenantId: number; logoUrl: string; primaryColor: string; secondaryColor: string }) =>
  apiRequest("/admin/whitelabel/branding", { method: "POST", body: payload });
export const getBranding = (tenantId: number) => apiRequest<any>("/admin/whitelabel/branding", { query: { tenantId } });
export const getPlatformEpics = () => apiRequest<PlatformEpic[]>("/platform/epics");

// === EPIC 23 SPONSORSHIP, ADVERTISING, AND MONETIZATION MARKETPLACE ===
export const getSponsorshipMarketplace = (params?: { city?: string; sportCategory?: string; maxAmount?: number }) =>
  apiRequest<any[]>("/sponsorship/marketplace", { query: params });
export const registerBrandPartner = (payload: {
  companyName: string;
  brandType?: string;
  gstNumber?: string;
  website?: string;
  email: string;
  phone?: string;
  city?: string;
  logoUrl?: string;
  documentUrl?: string;
}) => apiRequest<any>("/sponsorship/brands", { method: "POST", body: payload });
export const submitSponsorshipProposal = (opportunityId: number, payload: { brandId: number; message?: string; proposedAmount?: number; deliverables?: string; documentUrl?: string }) =>
  apiRequest<any>(`/sponsorship/opportunities/${opportunityId}/proposals`, { method: "POST", body: payload });
export const trackSponsoredCampaignEvent = (campaignId: number, payload: { eventType: "IMPRESSION" | "CLICK" | "CONVERSION"; userReference?: string; sessionId?: string; leadReference?: string; cost?: number }) =>
  apiRequest<any>(`/sponsorship/campaigns/${campaignId}/track`, { method: "POST", body: payload });
export const getAdminMonetizationDashboard = () => apiRequest<any>("/admin/monetization/dashboard");
export const getAdminMonetizationBrands = () => apiRequest<any[]>("/admin/monetization/brands");
export const getAdminMonetizationOpportunities = () => apiRequest<any[]>("/admin/monetization/opportunities");
export const getAdminMonetizationProposals = () => apiRequest<any[]>("/admin/monetization/proposals");
export const getAdminMonetizationCampaigns = () => apiRequest<any[]>("/admin/monetization/campaigns");
export const getAdminMonetizationLeads = () => apiRequest<any[]>("/admin/monetization/leads");
export const getAdminMonetizationContracts = () => apiRequest<any[]>("/admin/monetization/contracts");
export const getAdminMonetizationInvoices = () => apiRequest<any[]>("/admin/monetization/invoices");
export const getAdminMonetizationReports = () => apiRequest<any>("/admin/monetization/reports");

// === EPIC 24 MULTI-SPORT EXPANSION FRAMEWORK ===
export const getSportsCatalog = (activeOnly = false) => apiRequest<any[]>("/sports/catalog", { query: { activeOnly } });
export const getAdminSportsDashboard = () => apiRequest<any>("/admin/sports/dashboard");
export const getAdminSportsCatalog = () => apiRequest<any[]>("/admin/sports/catalog");
export const getAdminSportsAnalytics = () => apiRequest<any>("/admin/sports/analytics");
export const getAdminSportsAudit = () => apiRequest<any[]>("/admin/sports/audit");
export const seedAdminSportsDefaults = () => apiRequest<any>("/admin/sports/seed-defaults", { method: "POST" });
export const simulateSportRules = (payload: { sportCode: string; matchType?: string; playersPerSide?: number; payload?: Record<string, unknown> }) =>
  apiRequest<any>("/sports/rules/simulate", { method: "POST", body: payload });

// === EPIC 25 API MARKETPLACE, THIRD-PARTY INTEGRATIONS, AND DEVELOPER PLATFORM ===
export const getDeveloperPlatformDocs = () => apiRequest<any>("/developer-platform/docs");
export const getDeveloperMarketplace = () => apiRequest<any[]>("/developer-platform/marketplace");
export const registerDeveloper = (payload: { name: string; email: string; companyName?: string; website?: string; plan?: string }) =>
  apiRequest<any>("/developers/register", { method: "POST", body: payload });
export const getAdminDeveloperPlatformDashboard = () => apiRequest<any>("/admin/developer-platform/dashboard");
export const getAdminDevelopers = () => apiRequest<any[]>("/admin/developer-platform/developers");
export const getAdminDeveloperApplications = () => apiRequest<any[]>("/admin/developer-platform/applications");
export const createAdminDeveloperSandbox = (developerId: number) =>
  apiRequest<any>("/admin/developer-platform/applications/sandbox", { method: "POST", body: { developerId } });
export const getAdminDeveloperApiKeys = () => apiRequest<any[]>("/admin/developer-platform/api-keys");
export const getAdminDeveloperWebhooks = () => apiRequest<any[]>("/admin/developer-platform/webhooks");
export const getAdminDeveloperAnalytics = () => apiRequest<any>("/admin/developer-platform/analytics");
export const getAdminDeveloperBilling = () => apiRequest<any>("/admin/developer-platform/billing");
export const getAdminDeveloperMarketplace = () => apiRequest<any[]>("/admin/developer-platform/marketplace");
export const getAdminDeveloperAudit = () => apiRequest<any[]>("/admin/developer-platform/audit");

// === EPIC 26 SPORTS ACADEMY, COACHING, TRAINING, AND TALENT DEVELOPMENT ===
export const getAcademyMarketplace = (params?: { city?: string; sport?: string }) =>
  apiRequest<any[]>("/academies/marketplace", { query: params });
export const getCoachMarketplace = (params?: { city?: string; sport?: string; minRating?: number }) =>
  apiRequest<any[]>("/coaches/marketplace", { query: params });
export const getTrainingPrograms = (params?: { city?: string; sport?: string }) =>
  apiRequest<any[]>("/training/programs", { query: params });
export const getTalentDiscovery = (params?: { city?: string; sport?: string; ageGroup?: string }) =>
  apiRequest<any[]>("/talent/discovery", { query: params });
export const getAdminAcademyDashboard = () => apiRequest<any>("/admin/academy/dashboard");
export const getAdminAcademies = () => apiRequest<any[]>("/admin/academy/academies");
export const getAdminCoaches = () => apiRequest<any[]>("/admin/academy/coaches");
export const getAdminTrainingPrograms = () => apiRequest<any[]>("/admin/academy/programs");
export const getAdminTrainingBatches = () => apiRequest<any[]>("/admin/academy/batches");
export const getAdminAcademyEnrollments = () => apiRequest<any[]>("/admin/academy/enrollments");
export const getAdminAcademyAttendance = () => apiRequest<any[]>("/admin/academy/attendance");
export const getAdminAcademyAssessments = () => apiRequest<any[]>("/admin/academy/assessments");
export const getAdminAcademyVideos = () => apiRequest<any[]>("/admin/academy/videos");
export const getAdminTalentProfiles = () => apiRequest<any[]>("/admin/academy/talent");
export const getAdminAcademyCertificates = () => apiRequest<any[]>("/admin/academy/certificates");
export const getAdminAcademyAnalytics = () => apiRequest<any>("/admin/academy/analytics");
export const getAdminAcademyRevenue = () => apiRequest<any>("/admin/academy/revenue");
export const getAdminAcademyAudit = () => apiRequest<any[]>("/admin/academy/audit");

// === EPIC 27 CORPORATE SPORTS, WELLNESS, AND B2B ENGAGEMENT ===
export const getCorporateMarketplace = (params?: { city?: string }) =>
  apiRequest<any[]>("/corporates/marketplace", { query: params });
export const getAdminCorporateDashboard = () => apiRequest<any>("/admin/corporate/dashboard");
export const getAdminCorporateCompanies = () => apiRequest<any[]>("/admin/corporate/companies");
export const getAdminCorporateLocations = () => apiRequest<any[]>("/admin/corporate/locations");
export const getAdminCorporateDepartments = () => apiRequest<any[]>("/admin/corporate/departments");
export const getAdminCorporateEmployees = () => apiRequest<any[]>("/admin/corporate/employees");
export const getAdminCorporateTeams = () => apiRequest<any[]>("/admin/corporate/teams");
export const getAdminCorporateTeamMemberships = () => apiRequest<any[]>("/admin/corporate/team-memberships");
export const getAdminCorporateBookingPlans = () => apiRequest<any[]>("/admin/corporate/booking-plans");
export const getAdminCorporateTournaments = () => apiRequest<any[]>("/admin/corporate/tournaments");
export const getAdminCorporateWellnessPrograms = () => apiRequest<any[]>("/admin/corporate/wellness-programs");
export const getAdminCorporateWellnessActivities = () => apiRequest<any[]>("/admin/corporate/wellness-activities");
export const getAdminCorporateRewards = () => apiRequest<any[]>("/admin/corporate/rewards");
export const getAdminCorporateInvoices = () => apiRequest<any[]>("/admin/corporate/invoices");
export const getAdminCorporateContracts = () => apiRequest<any[]>("/admin/corporate/contracts");
export const getAdminCorporateLeagues = () => apiRequest<any[]>("/admin/corporate/leagues");
export const getAdminCorporateAnalytics = () => apiRequest<any>("/admin/corporate/analytics");
export const getAdminCorporateBilling = () => apiRequest<any>("/admin/corporate/billing");
export const getAdminCorporateAudit = () => apiRequest<any[]>("/admin/corporate/audit");

// === EPIC 36 GLOBAL EXPANSION, MULTI-TENANT SAAS, AND WHITE-LABEL PLATFORM ===
export const getSaasTenantContext = (tenantId?: number) =>
  apiRequest<SaasMap>("/saas/tenant-context", { query: tenantId ? { tenantId } : undefined });
export const getSaasMarketplaceModules = () => apiRequest<SaasMap[]>("/saas/marketplace/modules");
export const getSaasRegions = () => apiRequest<SaasMap[]>("/saas/regions");
export const getAdminSaasDashboard = () => apiRequest<SaasMap>("/admin/saas/dashboard");
export const createAdminSaasTenant = (payload: {
  name: string;
  domain: string;
  tenantType?: string;
  countryCode?: string;
  regionCode?: string;
  defaultLocale?: string;
  currency?: string;
  timezone?: string;
}) => apiRequest<SaasMap>("/admin/saas/tenants", { method: "POST", body: payload });
export const saveAdminSaasBranding = (payload: { tenantId: number; appName?: string; logoUrl?: string; primaryColor?: string; secondaryColor?: string; fontFamily?: string; published?: boolean }) =>
  apiRequest<SaasMap>("/admin/saas/branding", { method: "POST", body: payload });
export const addAdminSaasDomain = (payload: { tenantId: number; domain: string; routingMode?: string }) =>
  apiRequest<SaasMap>("/admin/saas/domains", { method: "POST", body: payload });
export const verifyAdminSaasDomain = (domainId: number) => apiRequest<SaasMap>(`/admin/saas/domains/${domainId}/verify`, { method: "POST" });
export const setAdminSaasFeature = (payload: { tenantId: number; featureKey: string; enabled: boolean; rolloutStrategy?: string; notes?: string }) =>
  apiRequest<SaasMap>("/admin/saas/features", { method: "POST", body: payload });
export const createAdminSaasSubscription = (payload: { tenantId: number; planCode: string; billingCycle?: string; price: number; currency?: string; autoRenew?: boolean }) =>
  apiRequest<SaasMap>("/admin/saas/subscriptions", { method: "POST", body: payload });
export const generateAdminSaasInvoice = (payload: { tenantId: number; subscriptionId?: number; amount: number; taxAmount?: number; currency?: string }) =>
  apiRequest<SaasMap>("/admin/saas/invoices", { method: "POST", body: payload });
export const payAdminSaasInvoice = (invoiceId: number, payload: { amount: number; paymentReference?: string }) =>
  apiRequest<SaasMap>(`/admin/saas/invoices/${invoiceId}/pay`, { method: "POST", body: payload });
export const installAdminSaasModule = (payload: { tenantId: number; moduleId: number; licenseType?: string }) =>
  apiRequest<SaasMap>("/admin/saas/marketplace/installations", { method: "POST", body: payload });
export const requestAdminSaasAppBuild = (payload: { tenantId: number; appType?: string; platform?: string; version?: string }) =>
  apiRequest<SaasMap>("/admin/saas/app-builds", { method: "POST", body: payload });
export const createAdminSaasComplianceRequest = (payload: { tenantId: number; regulation?: string; requestType?: string; subjectEmail?: string; notes?: string }) =>
  apiRequest<SaasMap>("/admin/saas/compliance", { method: "POST", body: payload });

// === EPIC 21 STAFF OPERATIONS APP ===
export const getStaffOpsDashboard = () => apiRequest<any>("/staff-ops/dashboard");
export const getStaffOpsSchedule = () => apiRequest<any[]>("/staff-ops/schedule");
export const getStaffOpsBookings = () => apiRequest<any[]>("/staff-ops/bookings");
export const staffOpsCheckIn = (payload: { bookingId?: number; bookingCode?: string; phone?: string; qrPayload?: string; note?: string }) =>
  apiRequest<any>("/staff-ops/check-ins", { method: "POST", body: payload });
export const getStaffOpsTasks = () => apiRequest<any[]>("/staff-ops/tasks");
export const updateStaffOpsTaskStatus = (taskId: number, status: string) =>
  apiRequest<any>(`/staff-ops/tasks/${taskId}/status`, { method: "PATCH", body: { status } });
export const getStaffOpsMaintenance = () => apiRequest<any[]>("/staff-ops/maintenance");
export const createStaffOpsMaintenance = (payload: { title: string; description?: string; category?: string; priority?: string; centerId?: number; turfId?: number; mediaUrls?: string[] }) =>
  apiRequest<any>("/staff-ops/maintenance", { method: "POST", body: payload });
export const updateStaffOpsMaintenanceStatus = (ticketId: number, status: string) =>
  apiRequest<any>(`/staff-ops/maintenance/${ticketId}/status`, { method: "PATCH", body: { status } });
export const startStaffOpsAttendance = (payload?: { centerId?: number; latitude?: number; longitude?: number; deviceId?: string; source?: string }) =>
  apiRequest<any>("/staff-ops/attendance/start", { method: "POST", body: payload || { source: "MOBILE" } });
export const endStaffOpsAttendance = (payload?: { latitude?: number; longitude?: number; deviceId?: string; source?: string }) =>
  apiRequest<any>("/staff-ops/attendance/end", { method: "POST", body: payload || { source: "MOBILE" } });
export const getStaffOpsAttendance = () => apiRequest<any[]>("/staff-ops/attendance");
export const getStaffOpsEquipment = () => apiRequest<any[]>("/staff-ops/equipment");
export const updateStaffOpsEquipment = (itemId: number, action: "issue" | "return" | "damage", payload?: { assignedTo?: string; note?: string }) =>
  apiRequest<any>(`/staff-ops/equipment/${itemId}/${action}`, { method: "POST", body: payload || {} });
export const getStaffOpsEmergencies = () => apiRequest<any[]>("/staff-ops/emergencies");
export const createStaffOpsEmergency = (payload: { type: string; severity?: string; message?: string; centerId?: number; turfId?: number }) =>
  apiRequest<any>("/staff-ops/emergencies", { method: "POST", body: payload });
export const resolveStaffOpsEmergency = (alertId: number) =>
  apiRequest<any>(`/staff-ops/emergencies/${alertId}/resolve`, { method: "PATCH" });
export const getStaffOpsAnalytics = () => apiRequest<any>("/staff-ops/analytics");
