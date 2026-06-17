export type RoleName = "PLAYER" | "OWNER" | "ADMIN" | string;

export type PortalKind = "player" | "owner";

export type SessionUser = {
  id?: number;
  name?: string;
  email?: string;
  phone?: string;
};

export type Session = {
  token: string;
  refreshToken?: string;
  role?: RoleName;
  user?: SessionUser;
};

export type LoginResponse = {
  accessToken?: string;
  token?: string;
  refreshToken?: string;
  role?: RoleName;
  user?: SessionUser;
};

export type SearchFilters = {
  query?: string;
  city?: string;
  date?: string;
  minPrice?: number;
  maxPrice?: number;
  boxType?: string;
  pitchType?: string;
  rating?: number | string;
  amenities?: string[];
  sortBy?: string;
  latitude?: number;
  longitude?: number;
  radius?: number;
  page?: number;
  size?: number;
};

export type TurfAmenity = {
  id?: number;
  amenity: string;
};

export type Turf = {
  id?: number;
  name: string;
  description?: string;
  city: string;
  location: string;
  latitude?: number;
  longitude?: number;
  pricePerHour: number;
  pitchType: "TURF" | "CONCRETE" | "MATTING" | string;
  boxType: "5v5" | "6v6" | "7v7" | "11v11" | string;
  status?: "ACTIVE" | "INACTIVE" | "PENDING" | "MAINTENANCE" | string;
  rating?: number;
  totalReviews?: number;
  images?: string[];
  amenities?: TurfAmenity[];
  distance?: number;
  available?: boolean;
};

export type Slot = {
  id: number;
  startTime: string;
  endTime: string;
  price: number;
  isActive: boolean;
};

export type SlotAvailability = {
  slotId: number;
  startTime: string;
  endTime: string;
  price: number;
  available: boolean;
  blocked: boolean;
  locked?: boolean;
  status?: "AVAILABLE" | "LOCKED" | "BOOKED" | "BLOCKED" | "EXPIRED" | "UNAVAILABLE" | string;
};

export type Booking = {
  id: number;
  playerId?: number;
  playerName?: string;
  playerEmail?: string;
  playerPhone?: string;
  turfId: number;
  turfName?: string;
  turfCity?: string;
  turfLocation?: string;
  slotId: number;
  startTime?: string;
  endTime?: string;
  bookingDate: string;
  amount?: number;
  totalAmount?: number;
  status: "PENDING" | "CONFIRMED" | "CHECKED_IN" | "CANCELLED" | "COMPLETED" | "EXPIRED" | "NO_SHOW" | "REFUNDED" | string;
  paymentStatus?: "PENDING" | "PAID" | "FAILED" | "REFUNDED" | string;
  attendanceStatus?: "ARRIVED" | "LATE" | "NO_SHOW" | "CANCELLED" | string;
  checkedInAt?: string;
  createdAt?: string;
};

export type Wallet = {
  id?: number;
  balance?: number;
  transactions?: WalletTransaction[];
};

export type WalletTransaction = {
  id: number;
  type?: string;
  amount: number;
  status?: string;
  description?: string;
  createdAt?: string;
};

export type Coupon = {
  id?: number;
  code: string;
  description?: string;
  discountAmount?: number;
  discountPercent?: number;
  minBookingAmount?: number;
  active?: boolean;
  expiryDate?: string;
};

export type CouponValidation = {
  valid: boolean;
  code?: string;
  discountAmount?: number;
  finalAmount?: number;
  message?: string;
};

  export type NotificationItem = {
    id: number;
    title: string;
    message: string;
  status: "PENDING" | "SENT" | "READ" | string;
  deliveryStatus?: "PENDING" | "SENT" | "FAILED" | string;
  read?: boolean;
  archived?: boolean;
  channel?: "EMAIL" | "PUSH" | "SMS" | "WHATSAPP" | string;
  category?: "BOOKING" | "PAYMENT" | "CANCELLATION" | "TEAM" | "MATCH" | "TOURNAMENT" | "REVIEW" | "SOCIAL" | "MARKETING" | "GENERAL" | string;
  type?: "BOOKING" | "PAYMENT" | "CANCELLATION" | "TEAM" | "MATCH" | "TOURNAMENT" | "REVIEW" | "SOCIAL" | "MARKETING" | "GENERAL" | string;
  priority?: "LOW" | "NORMAL" | "HIGH" | "CRITICAL" | string;
  campaignId?: number | null;
  deepLink?: string | null;
  retryCount?: number;
  createdAt?: string;
  readAt?: string | null;
    archivedAt?: string | null;
  };

  export type NotificationSummary = {
    total: number;
    unread: number;
    read: number;
    archived: number;
    pending: number;
    failed: number;
    byChannel: Record<string, number>;
    byCategory: Record<string, number>;
  };

  export type NotificationAnnouncement = {
    id: number;
    title: string;
    message: string;
    priority: string;
    displayLocation: string;
    status: string;
    createdAt?: string;
    updatedAt?: string;
  };

export type PersonalizationPayload = {
  recommendedTurfs: Turf[];
  recentSearches: Array<{ id: number; city?: string; date?: string; boxType?: string; pitchType?: string }>;
  recentlyBooked: Booking[];
  preferredLocations: string[];
  recommendedTimings: string[];
};

export type FrequentBooking = {
  sourceBookingId: number;
  turfId: number;
  turfName: string;
  city?: string;
  slotId: number;
  startTime?: string;
  endTime?: string;
  amount?: number;
  timesBooked: number;
  lastBookedDate?: string;
  nextSuggestedDate?: string;
  recommendationReason?: string;
};

export type BookingPrediction = {
  turfId?: number;
  turfName?: string;
  slotId?: number;
  predictedDate?: string;
  startTime?: string;
  endTime?: string;
  confidence?: number;
  reason?: string;
};

export type RecurringBooking = {
  id: number;
  turfId: number;
  turfName: string;
  slotId: number;
  startTime?: string;
  endTime?: string;
  frequency: string;
  preferredDay?: string;
  nextBookingDate?: string;
  autoConfirm?: boolean;
  status: string;
};

export type BookingTemplate = {
  id: number;
  templateName: string;
  turfId: number;
  turfName: string;
  preferredDay?: string;
  preferredTime?: string;
  durationMinutes?: number;
  players?: number;
  status?: string;
};

export type AutoWaitlist = {
  id: number;
  slotId: number;
  turfName?: string;
  preferredDate: string;
  autoBookEnabled: boolean;
  status: string;
};

export type BookingIntelligence = {
  frequentBookings: number;
  recurringBookings: number;
  templates: number;
  autoWaitlists: number;
  conflicts: number;
  rebookReadiness: number;
  topFrequentBookings?: FrequentBooking[];
  predictions?: BookingPrediction[];
};

export type WeatherRiskLevel = "LOW" | "MEDIUM" | "HIGH" | "UNSAFE" | string;

export type Forecast = {
  city: string;
  date: string;
  hour: number;
  temperature: number;
  rainProbability: number;
  humidity: number;
  windSpeed: number;
  uvIndex: number;
  summary: string;
  riskLevel: WeatherRiskLevel;
};

export type Playability = {
  turfId: number;
  turfName: string;
  slotId: number;
  date: string;
  startTime?: string;
  endTime?: string;
  score: number;
  riskLevel: WeatherRiskLevel;
  rainProbability: number;
  temperature: number;
  reason: string;
  recommendation: string;
};

export type WeatherPolicy = {
  rainRefundAllowed: boolean;
  flexibleReschedule: boolean;
  rainThreshold: number;
  heatThresholdCelsius: number;
  refundType: "FULL" | "PARTIAL" | "WALLET_CREDIT" | "RESCHEDULE_ONLY" | string;
};

export type WeatherDashboard = {
  city: string;
  current: Forecast;
  forecast: Forecast[];
  recommendations: Playability[];
  highRiskBookings: number;
  weatherRefunds: number;
  insight: string;
};

export type GamificationActivityType = "BOOKING" | "MATCH_PLAYED" | "ATTENDANCE" | "WIN" | "REVIEW" | "REFERRAL" | "MVP" | "CHALLENGE";
export type ChallengeStatus = "ACTIVE" | "COMPLETED" | "EXPIRED" | string;
export type GamificationRewardStatus = "LOCKED" | "AVAILABLE" | "CLAIMED" | string;
export type LeaderboardType = "HIGHEST_XP" | "LONGEST_STREAK" | "MOST_MATCHES" | "MOST_ACTIVE" | string;

export type GamificationProfile = {
  xp: { userId: number; playerName: string; xp: number; level: number; nextLevelXp: number; progressPercent: number; title: string; lastActivity?: string };
  streaks: Array<{ streakType: string; currentStreak: number; bestStreak: number; lastActivityDate?: string; label: string }>;
  achievements: Array<{ id: number; name: string; category: string; triggerType: string; threshold: number; xpReward: number; earned: boolean; earnedAt?: string }>;
  badges: Array<{ id: number; badge: string; earnedAt?: string }>;
  challenges: Array<{ id: number; title: string; description?: string; activityType: string; target: number; progress: number; xpReward: number; rewardLabel?: string; status: ChallengeStatus; expiresAt?: string; progressPercent: number }>;
  rewards: Array<{ id: number; rewardType: string; label: string; status: GamificationRewardStatus; unlockedAt?: string; claimedAt?: string }>;
  leaderboard: GamificationLeaderboardEntry[];
  seasonalEvents: Array<{ id: number; name: string; description?: string; startDate: string; endDate: string }>;
  insight: string;
};

export type GamificationLeaderboardEntry = {
  userId: number;
  playerName: string;
  rank: number;
  score: number;
  level: number;
  city: string;
  leaderboardType: LeaderboardType;
};

export type GamificationAnalytics = {
  activePlayers: number;
  completedChallenges: number;
  availableRewards: number;
  challengeCompletionRate: number;
  topPlayers: GamificationLeaderboardEntry[];
  teamCompetitions: Array<{ teamId: number; teamName: string; points: number; rank: number; seasonName: string }>;
  insight: string;
};

export type TeamMember = {
  id: number;
  userId: number;
  name: string;
  email: string;
  role: "CAPTAIN" | "VICE_CAPTAIN" | "PLAYER" | string;
  status: "INVITED" | "ACTIVE" | "DECLINED" | "LEFT" | "REMOVED" | string;
};

export type Team = {
  id: number;
  name: string;
  sport: string;
  city: string;
  description?: string;
  logoUrl?: string;
  visibility?: "PUBLIC" | "PRIVATE" | string;
  maxMembers: number;
  memberCount: number;
  openSpots?: number;
  reputationScore?: number;
  captainId: number;
  captainName: string;
  members?: TeamMember[];
};

export type TeamJoinRequest = {
  id: number;
  teamId: number;
  teamName: string;
  requesterId: number;
  requesterName: string;
  reviewerId?: number;
  reviewerName?: string;
  message?: string;
  status: "PENDING" | "ACCEPTED" | "DECLINED" | string;
  createdAt?: string;
  updatedAt?: string;
};

export type TeamWorkspace = {
  teamId: number;
  teamName: string;
  city: string;
  sport: string;
  visibility: string;
  teamType: string;
  bannerUrl?: string;
  goals?: string;
  memberCount: number;
  availableToday: number;
  maybeToday: number;
  unavailableToday: number;
  teamBalance: number;
  attendanceRate: number;
};

export type TeamAnalytics = {
  totalTeams: number;
  activeMembers: number;
  availabilityResponses: number;
  polls: number;
  teamWalletBalance: number;
  averageAttendanceRate: number;
  mostActiveTeams: TeamWorkspace[];
};

export type PublicMatch = {
  id: number;
  creatorId: number;
  creatorName: string;
  turfId?: number;
  turfName?: string;
  title: string;
  sport: string;
  city: string;
  location: string;
  matchDate: string;
  startTime: string;
  endTime?: string;
  maxPlayers: number;
  joinedPlayers: number;
  visibility?: "PUBLIC" | "PRIVATE" | "PAID" | string;
  format?: "FRIENDLY" | "PRACTICE" | "T20" | "ONE_DAY" | "BOX_CRICKET" | string;
  overs?: number;
  entryFee?: number;
  homeTeamId?: number;
  homeTeamName?: string;
  awayTeamId?: number;
  awayTeamName?: string;
  winnerTeamId?: number;
  winnerTeamName?: string;
  homeScore?: number;
  awayScore?: number;
  resultMargin?: string;
  challengeMessage?: string;
  resultSummary?: string;
  acceptedAt?: string;
  scheduledAt?: string;
  startedAt?: string;
  completedAt?: string;
  notes?: string;
  status: "PENDING" | "ACCEPTED" | "SCHEDULED" | "OPEN" | "FULL" | "LIVE" | "REJECTED" | "CANCELLED" | "COMPLETED" | string;
};

export type TeamMatchStats = {
  teamId: number;
  teamName: string;
  matchesPlayed: number;
  wins: number;
  losses: number;
  draws: number;
  winPercentage: number;
  pendingChallenges: number;
  upcomingMatches: number;
  rivalries: {
    opponentTeamId: number;
    opponentTeamName: string;
    matchesPlayed: number;
    wins: number;
    losses: number;
  }[];
};

export type FeedEvent = {
  id: number;
  actorId: number;
  actorName: string;
  eventType:
    | "TEAM_CREATED"
    | "TEAM_INVITE_SENT"
    | "TEAM_JOINED"
    | "MATCH_CREATED"
    | "MATCH_JOINED"
    | "PROFILE_UPDATED"
    | "SOCIAL_POST_CREATED"
    | "SOCIAL_POST_EDITED"
    | "SOCIAL_POST_REACTED"
    | "SOCIAL_COMMENTED"
    | "SOCIAL_POST_REPORTED"
    | string;
  title: string;
  summary: string;
  entityId?: number;
  entityType?: string;
  deepLink?: string;
  visibility?: string;
  status?: string;
  mediaUrls?: string[];
  reactionCount?: number;
  commentCount?: number;
  shareCount?: number;
  viewCount?: number;
  engagementScore?: number;
  viewerReaction?: string | null;
  ownPost?: boolean;
  editedAt?: string;
  createdAt?: string;
};

export type PlayerAvailability = {
  id?: number;
  userId: number;
  userName: string;
  availableDays?: string;
  preferredWindow?: string;
  homeCity?: string;
  canTravel?: boolean;
  notes?: string;
  updatedAt?: string;
};

export type PlayerDiscover = {
  userId: number;
  name: string;
  email: string;
  city?: string;
  preferredTime?: string;
  battingStyle?: string;
  bowlingStyle?: string;
  position?: string;
  experience?: string;
  skillLevel?: string;
  trustScore?: number;
  networkScore?: number;
  matchScore?: number;
  connectionStatus?: string;
  recommendationReason?: string;
  rating: number;
  matches: number;
  wins: number;
  availability?: PlayerAvailability | null;
};

export type PlayerConnection = {
  id: number;
  fromUserId: number;
  fromUserName: string;
  toUserId: number;
  toUserName: string;
  type: string;
  status: string;
  message?: string;
  createdAt?: string;
};

export type TeamRecruitment = {
  id: number;
  teamId: number;
  teamName: string;
  captainId: number;
  captainName: string;
  roleNeeded: string;
  skillLevel?: string;
  city?: string;
  availability?: string;
  description?: string;
  matchDate?: string;
  status: string;
  applicationCount: number;
};

export type CommunityAnalytics = {
  discoverablePlayers: number;
  acceptedConnections: number;
  pendingConnections: number;
  openRecruitments: number;
  pendingApplications: number;
  feedEvents: number;
  activePosts?: number;
  comments?: number;
  moderationItems?: number;
  engagementRate?: number;
  insight: string;
};

export type MatchmakingCandidate = {
  userId: number;
  name: string;
  city?: string;
  position?: string;
  preferredTime?: string;
  skillLevel?: string;
  trustScore: number;
  rating?: number;
  matches?: number;
  recommendationScore: number;
  reason: string;
};

export type MatchmakingMatch = {
  id: number;
  title: string;
  city: string;
  location: string;
  matchDate: string;
  startTime: string;
  endTime?: string;
  maxPlayers: number;
  joinedPlayers: number;
  slotsLeft: number;
  skillLevel?: string;
  autoApprove: boolean;
  trustedOnly: boolean;
  status: "OPEN" | "FULL" | "CANCELLED" | "COMPLETED" | string;
  matchScore: number;
  recommendationReason: string;
  match: PublicMatch;
  candidates: MatchmakingCandidate[];
};

export type MatchmakingAnalytics = {
  openMatches: number;
  fullMatches: number;
  totalParticipants: number;
  averageFillRate: number;
  mostFillableMatches: MatchmakingMatch[];
};

export type ResaleListing = {
  id: number;
  bookingId: number;
  sellerId: number;
  sellerName: string;
  buyerId?: number;
  buyerName?: string;
  turfId: number;
  turfName: string;
  city: string;
  location: string;
  slotId: number;
  bookingDate: string;
  startTime: string;
  endTime: string;
  price: number;
  originalAmount: number;
  commissionAmount: number;
  status: "ACTIVE" | "SOLD" | "EXPIRED" | "CANCELLED" | string;
  expiresAt: string;
  soldAt?: string;
  notes?: string;
  sellerTrustScore: number;
  buyerTrustScore?: number;
  recommendationScore: number;
  recommendationReason: string;
  createdAt?: string;
};

export type SlotWaitlist = {
  id: number;
  slotId: number;
  turfId: number;
  turfName: string;
  city: string;
  date: string;
  startTime: string;
  endTime: string;
  priority: number;
  active: boolean;
  createdAt?: string;
};

export type ResaleRule = {
  id?: number;
  ownerId: number;
  allowResale: boolean;
  autoApproveTransfer: boolean;
  restrictLowTrustUsers: boolean;
  maxMarkupPercent: number;
  minHoursBeforeStart: number;
  updatedAt?: string;
};

export type ResaleAnalytics = {
  activeListings: number;
  soldListings: number;
  expiredListings: number;
  waitlistUsers: number;
  marketplaceGmv: number;
  recoveredRevenue: number;
  successRate: number;
  recentListings: ResaleListing[];
};

export type CommerceInventory = {
  id: number;
  variantLabel: string;
  sku: string;
  stockOnHand: number;
  reservedStock: number;
  availableStock: number;
  lowStockThreshold: number;
  lowStock: boolean;
};

export type CommerceProduct = {
  id: number;
  sellerId: number;
  sellerName: string;
  name: string;
  brand?: string;
  sku: string;
  category: string;
  sport: string;
  description?: string;
  price: number;
  mrp?: number;
  productType: "SALE" | "RENTAL" | "AFFILIATE" | "CUSTOM_JERSEY" | string;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED" | string;
  imageUrl?: string;
  affiliateUrl?: string;
  rentalFeePerDay?: number;
  depositAmount?: number;
  rating?: number;
  featured: boolean;
  availableStock: number;
  lowStockThreshold: number;
  variants: CommerceInventory[];
  updatedAt?: string;
};

export type CommerceCartItem = {
  id: number;
  product: CommerceProduct;
  inventory: CommerceInventory;
  quantity: number;
  rentalStart?: string;
  rentalEnd?: string;
  lineTotal: number;
};

export type CommerceCart = {
  items: CommerceCartItem[];
  subtotal: number;
  rentalDeposits: number;
  itemCount: number;
};

export type CommerceOrder = {
  id: number;
  orderNumber: string;
  status: string;
  subtotal: number;
  discount: number;
  loyaltyPointsRedeemed: number;
  shippingFee: number;
  total: number;
  paymentStatus: string;
  createdAt?: string;
};

export type TravelPackage = {
  id: number;
  packageCode: string;
  title: string;
  sport: string;
  originCity?: string;
  destination: string;
  packageType: string;
  startDate: string;
  endDate: string;
  pricePerPerson: number;
  capacity: number;
  bookedCount: number;
  availableCapacity: number;
  includesAccommodation: boolean;
  includesTransport: boolean;
  status: string;
  itinerary?: string;
};

export type TravelTicketType = {
  id: number;
  eventId: number;
  name: string;
  price: number;
  totalInventory: number;
  soldInventory: number;
  availableInventory: number;
  active: boolean;
};

export type TravelEvent = {
  id: number;
  title: string;
  eventType: "EVENT" | "CAMP" | "EXPERIENCE" | "FESTIVAL" | string;
  sport: string;
  city: string;
  venue?: string;
  startsAt: string;
  endsAt: string;
  capacity: number;
  registeredCount: number;
  availableCapacity: number;
  basePrice: number;
  status: string;
  description?: string;
  ticketTypes: TravelTicketType[];
};

export type TravelAccommodation = {
  id: number;
  partnerName?: string;
  name: string;
  accommodationType: string;
  city: string;
  roomType: string;
  pricePerNight: number;
  totalRooms: number;
  bookedRooms: number;
  availableRooms: number;
  status: string;
};

export type CareerMap = Record<string, any>;

export type GovernanceMap = Record<string, any>;

export type SuperAppMap = Record<string, any>;

export type SportsOsMap = Record<string, any>;

export type FanEconomyMap = Record<string, any>;

export type SaasMap = Record<string, any>;

export type CareerProfilePayload = {
  playerName: string;
  playerEmail: string;
  phone?: string;
  sport?: string;
  specialization?: string;
  playerRole?: string;
  city?: string;
  ageGroup?: string;
  bio?: string;
  statisticsSummary?: string;
  certificatesSummary?: string;
  scoutOptIn?: boolean;
  visibility?: string;
};

export type OwnerInsights = {
  revenue: {
    todayRevenue: number;
    weekRevenue: number;
    lastWeekRevenue: number;
    monthRevenue: number;
    averageBookingValue: number;
    weeklyGrowthPercent: number;
    dailyTrend: Array<{ date: string; bookings: number; revenue: number }>;
  };
  occupancy: {
    occupancyPercent: number;
    bookedSlots: number;
    availableSlotCapacity: number;
    unusedSlotCapacity: number;
    peakUtilizationPercent: number;
  };
  heatmap: Array<{ dayOfWeek: string; hour: number; hourLabel: string; bookingCount: number; utilizationPercent: number; intensity: string }>;
  customers: Array<{ customerId: number; customerName: string; bookings: number; lifetimeValue: number; lastVisit: string; segment: string; trustScore: number }>;
  pricingSuggestions: Array<{ turfId: number; turfName: string; slotId: number; currentPrice: number; recommendedPrice: number; reason: string; confidence: number }>;
  bookingPatterns: Array<{ label: string; metric: string; insight: string; direction: string }>;
  profit: { revenue: number; expenses: number; profit: number; marginPercent: number };
  forecast: { period: string; expectedRevenue: number; expectedOccupancy: number; demandLevel: string; insight: string };
  promotions: Array<{ title: string; description: string; suggestedDiscountPercent: number; expectedImpact: string }>;
  community: { repeatCustomers: number; frequentCaptains: number; activeTeams: number; communityScore: string };
  coach: { answer: string; recommendedActions: string[]; confidence: number };
  recentExpenses: Array<{ id: number; type: string; amount: number; expenseDate: string; note?: string }>;
};

export type PaymentSplit = {
  id: number;
  bookingId: number;
  turfName?: string;
  bookingDate?: string;
  startTime?: string;
  endTime?: string;
  userId: number;
  playerName: string;
  amount: number;
  status: "PENDING" | "PAID" | "OVERDUE" | "FAILED" | "REFUNDED" | string;
  dueDate?: string;
  paidAt?: string;
  paymentLink?: string;
  paymentReference?: string;
};

export type PaymentRule = {
  id?: number;
  ownerId: number;
  minimumPercentage: number;
  autoCancel: boolean;
  fullPaymentRequired: boolean;
  reminderHoursBefore: number;
};

export type SplitAnalytics = {
  totalSplits: number;
  paidSplits: number;
  pendingSplits: number;
  totalCollected: number;
  totalPending: number;
  collectionSuccessRate: number;
  insight: string;
};

export type ChatMessage = {
  id: number;
  roomType: "TEAM" | "MATCH" | "DIRECT" | string;
  roomId: number;
  senderId: number;
  senderName: string;
  message: string;
  messageType?: string;
  mediaUrl?: string;
  deliveredAt?: string;
  readAt?: string;
  createdAt?: string;
};

export type Tournament = {
  id: number;
  organizerId: number;
  organizerName: string;
  turfId?: number;
  turfName?: string;
  name: string;
  sport: string;
  city: string;
  venue: string;
  description?: string;
  format: "LEAGUE" | "KNOCKOUT" | string;
  status: "DRAFT" | "UPCOMING" | "REGISTRATION_OPEN" | "REGISTRATION_CLOSED" | "ONGOING" | "COMPLETED" | "CANCELLED" | string;
  maxTeams: number;
  registeredTeams: number;
  entryFee: number;
  prizePool?: number;
  startDate: string;
  endDate?: string;
};

export type TournamentFixture = {
  id: number;
  tournamentId: number;
  teamAId: number;
  teamAName: string;
  teamBId: number;
  teamBName: string;
  winnerTeamId?: number;
  winnerTeamName?: string;
  round: number;
  matchDate?: string;
  startTime?: string;
  teamAScore?: number;
  teamBScore?: number;
  stageLabel?: string;
  knockoutStage?: boolean;
  status: "SCHEDULED" | "LIVE" | "COMPLETED" | "CANCELLED" | string;
};

export type TournamentStanding = {
  teamId: number;
  teamName: string;
  played: number;
  wins: number;
  losses: number;
  draws: number;
  points: number;
  runsFor: number;
  runsAgainst: number;
  netRuns: number;
  netRunRate: number;
};

export type LiveScore = {
  id: number;
  fixtureId?: number;
  publicMatchId?: number;
  scorerId: number;
  scorerName: string;
  title: string;
  teamAName: string;
  teamBName: string;
  battingTeam: string;
  bowlingTeam: string;
  innings: number;
  runs: number;
  wickets: number;
  ballsBowled: number;
  overs: string;
  extras: number;
  targetRuns?: number;
  maxOvers: number;
  runRate: number;
  projectedScore: number;
  status: "NOT_STARTED" | "LIVE" | "INNINGS_BREAK" | "COMPLETED" | string;
  updatedAt?: string;
  locked?: boolean;
  tie?: boolean;
  superOver?: boolean;
  winnerTeamName?: string;
  resultSummary?: string;
  shareUrl?: string;
};

export type ScoreEvent = {
  id: number;
  eventSequence: number;
  runs: number;
  extraRuns: number;
  extraType?: string;
  wicket: boolean;
  wicketType?: string;
  batter?: string;
  bowler?: string;
  note?: string;
  createdAt?: string;
};

export type LiveScoreDetail = {
  score: LiveScore;
  events: ScoreEvent[];
  innings?: Array<{
    inningsNumber: number;
    battingTeamName: string;
    runs: number;
    wickets: number;
    ballsBowled: number;
    overs: string;
    extras: number;
  }>;
  battingScorecard?: Array<{
    playerName: string;
    runs: number;
    balls: number;
    fours: number;
    sixes: number;
    strikeRate: number;
    out: boolean;
    dismissal?: string;
  }>;
  bowlingScorecard?: Array<{
    playerName: string;
    overs: string;
    balls: number;
    runsConceded: number;
    wickets: number;
    economy: number;
  }>;
  recentOvers?: Array<{
    overNumber: number;
    label: string;
    runs: number;
    wickets: number;
    events: ScoreEvent[];
  }>;
  fallOfWickets?: Array<{
    wicketNumber: number;
    score: number;
    over: string;
    batter: string;
    wicketType?: string;
  }>;
  resultSummary?: string;
  winnerTeamName?: string;
  verificationStatus?: string;
  streamUrl?: string;
  shareUrl?: string;
  auditTrail?: string[];
  strikerName?: string;
  strikerRuns?: number;
  strikerBalls?: number;
  strikerFours?: number;
  strikerSixes?: number;
  nonStrikerName?: string;
  nonStrikerRuns?: number;
  nonStrikerBalls?: number;
  nonStrikerFours?: number;
  nonStrikerSixes?: number;
  bowlerName?: string;
  bowlerOvers?: number;
  bowlerRuns?: number;
  bowlerWickets?: number;
  partnershipRuns?: number;
  partnershipBalls?: number;
  requiredRunRate?: number;
};

export type PlayerPerformance = {
  profile: {
    userId: number;
    name: string;
    email?: string;
    phone?: string;
    photoUrl?: string;
    bio?: string;
    city?: string;
    battingStyle?: string;
    bowlingStyle?: string;
    position?: string;
    experience?: string;
    matches: number;
    wins: number;
    mvp: number;
    rating: number;
    profileCompletionPercentage: number;
  };
  career: {
    matches: number;
    wins: number;
    losses: number;
    draws: number;
    mvpAwards: number;
    reputationScore: number;
    impactScore: number;
    consistencyScore: number;
  };
  batting: {
    innings: number;
    runs: number;
    balls: number;
    fours: number;
    sixes: number;
    highestScore: number;
    average: number;
    strikeRate: number;
    fifties: number;
    hundreds: number;
  };
  bowling: {
    innings: number;
    wickets: number;
    balls: number;
    overs: string;
    runsConceded: number;
    average: number;
    economyRate: number;
    strikeRate: number;
    bestFigures: string;
  };
  fielding: {
    catches: number;
    runouts: number;
    dismissals: number;
  };
  recentMatches: Array<{
    liveScoreId?: number;
    title: string;
    status?: string;
    result: string;
    runs: number;
    balls: number;
    fours: number;
    sixes: number;
    wickets: number;
    overs: string;
    runsConceded: number;
    catches: number;
    runouts: number;
    playedAt?: string;
  }>;
  achievements: Array<{
    code: string;
    name: string;
    description: string;
    unlocked: boolean;
    progress: number;
    target: number;
  }>;
  timeline: Array<{
    type: string;
    title: string;
    description: string;
    liveScoreId?: number;
    occurredAt?: string;
  }>;
  insight: string;
};

export type PlayerPerformanceLeaderboardEntry = {
  userId: number;
  playerName: string;
  city?: string;
  rank: number;
  leaderboardType: string;
  score: number;
  statLabel: string;
  matches: number;
  runs: number;
  wickets: number;
  impactScore: number;
};

export type UserProfile = {
  id: number;
  name: string;
  email: string;
  phone: string;
  preferredLanguage?: string;
  preferredTheme?: "light" | "dark" | "system" | string;
  profileCompletionPercentage?: number;
  roles?: string[];
};

export type PlayerSettings = {
  profile: {
    userId: number;
    displayName: string;
    email: string;
    phone: string;
    bio?: string | null;
    city?: string | null;
    position?: string | null;
    skillLevel?: string | null;
    visibility?: string;
    completionPercentage?: number | null;
  };
  notifications: {
    push: boolean;
    email: boolean;
    sms: boolean;
    whatsapp: boolean;
    frequency: string;
  };
  privacy: {
    profileVisibility: string;
    showTrustScore: boolean;
    showStats: boolean;
    showAvailability: boolean;
    showLocation: boolean;
  };
  booking: {
    preferredDay?: string | null;
    preferredTime?: string | null;
    preferredDurationMinutes?: number | null;
    preferredCity?: string | null;
    autoWaitlistEnabled: boolean;
    quickRebookEnabled: boolean;
  };
  matchmaking: {
    competitionLevel: string;
    preferredSkillLevel: string;
    trustedOnly: boolean;
    nearbyOnly: boolean;
  };
  community: {
    allowTeamInvites: boolean;
    allowFriendRequests: boolean;
    allowMessages: boolean;
  };
  payments: {
    defaultPaymentMethod?: string | null;
    savedUpiId?: string | null;
    autoPaySplits: boolean;
  };
  localization: {
    language: string;
    regionCity: string;
    currency: string;
    timezone: string;
  };
  devices: {
    activeSessions: number;
    currentDevice: string;
    lastActivity: string;
  };
  data: {
    downloadDataRequested: boolean;
    lastExportRequestedAt?: string | null;
    exportFormats: string[];
  };
  smartSuggestions: string[];
};

export type OwnerSettings = {
  ownerId?: number;
  ownerName?: string;
  ownerEmail?: string;
  ownerPhone?: string;
  accountStatus?: string;
  preferredLanguage?: string;
  preferredTheme?: string;
  businessName: string;
  supportPhone: string;
  businessAddress: string;
  gstNumber: string;
  businessLogoUrl: string;
  supportEmail: string;
  businessType: string;
  operatingHours: string;
  defaultSlotDurationMinutes: number;
  openingTime: string;
  closingTime: string;
  indoorOutdoor: string;
  amenities: string;
  advanceBookingDays: number;
  minimumBookingMinutes: number;
  allowInstantBooking: boolean;
  approvalRequired: boolean;
  trustedPlayersOnly: boolean;
  cancellationPolicy: string;
  dynamicPricingEnabled: boolean;
  weekendPricingEnabled: boolean;
  peakPricingEnabled: boolean;
  pricingMode: string;
  peakMultiplier: number;
  weekendMultiplier: number;
  staffInviteEmail: string;
  staffRole: string;
  staffPermissions: string;
  bankAccountName: string;
  bankAccountNumber: string;
  bankIfsc: string;
  upiId: string;
  settlementSchedule: string;
  bookingAlerts: boolean;
  paymentAlerts: boolean;
  reviewAlerts: boolean;
  marketingAlerts: boolean;
  promotionWhatsappEnabled: boolean;
  reminderLeadMinutes: number;
  partialPaymentEnabled: boolean;
  minimumPaymentPercent: number;
  refundMode: string;
  rainRefundAllowed: boolean;
  freeWeatherReschedule: boolean;
  weatherRiskAlerts: boolean;
  rainThresholdPercent: number;
  resaleEnabled: boolean;
  transferEnabled: boolean;
  maxResaleMarkupPercent: number;
  restrictSuspiciousAccounts: boolean;
  allowPublicTournaments: boolean;
  communityVisibility: string;
  teamRegistrationRule: string;
  brandPrimaryColor: string;
  brandTheme: string;
  compactDashboard: boolean;
  twoFactorEnabled: boolean;
  sessionTimeoutMinutes: number;
  ipRestrictions: string;
  autoPricingEnabled: boolean;
  autoRescheduleEnabled: boolean;
  aiCoachEnabled: boolean;
  smartPromotionsEnabled: boolean;
  revenueExportEnabled: boolean;
  gstReportsEnabled: boolean;
  exportFormat: string;
  dataExportRequested: boolean;
  readinessScore?: number;
  lastExportRequestedAt?: string | null;
};

export type MediaAsset = {
  id?: number;
  ownerType: "USER" | "OWNER" | "TEAM" | string;
  ownerId: number;
  mediaType: "PROFILE_PHOTO" | "BUSINESS_LOGO" | "TEAM_LOGO" | string;
  imageUrl: string;
  thumbnailUrl: string;
  visibility: string;
  moderationStatus: string;
  moderationReason?: string | null;
  contentType: string;
  fileSizeBytes: number;
  uploadedAt?: string | null;
};

export type OwnerDashboard = {
  todayBookings: number;
  totalBookings: number;
  todayRevenue: number;
  monthlyRevenue: number;
  totalRevenue: number;
  occupancyPercent: number;
  cancellationPercent: number;
  activeTurfs: number;
  inactiveTurfs: number;
  maintenanceTurfs: number;
  popularTurf?: Turf | null;
  revenueSeries: Array<{ date: string; bookings: number; revenue: number }>;
  upcomingBookings: Booking[];
  recentReviews: Array<{ id: number; rating: number; comment?: string; turfName?: string; turfId: number }>;
};

export type OwnerFinanceDashboard = {
  todayRevenue: number;
  monthlyRevenue: number;
  lifetimeRevenue: number;
  grossRevenue: number;
  refundedAmount: number;
  netCollectedRevenue: number;
  commissionRate: number;
  commissionDue: number;
  gstRate: number;
  gstOnCommission: number;
  netPayable: number;
  pendingSettlementAmount: number;
  paidSettlementAmount: number;
  paidBookings: number;
  refundedPayments: number;
  pendingSettlements: number;
  paidSettlements: number;
  insight?: string;
  revenueTrend?: Array<{ date: string; revenue: number; payments: number }>;
  turfRevenue?: Array<{ turfId: number; turfName: string; revenue: number; paidBookings: number; averageBookingValue: number }>;
  settlements?: Array<{ id: number; bookingId?: number; netAmount: number; grossAmount: number; commission: number; status: string; reference?: string }>;
  transactions?: Array<{ paymentId: number; bookingId?: number; turfName?: string; playerName?: string; amount: number; refundedAmount: number; status: string; paidAt?: string }>;
};

export type Review = {
  id: number;
  userId?: number;
  userName?: string;
  rating: number;
  title?: string;
  comment?: string;
  status?: string;
  moderationReason?: string;
  verifiedBooking?: boolean;
  ownerReply?: string;
  ownerReplyAt?: string;
  helpfulCount?: number;
  notHelpfulCount?: number;
  turfId?: number;
  turfName?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type PlatformEpicSurface = {
  surface: string;
  status: string;
  owner: string;
  capabilities: string[];
};

export type PlatformEpicChecklist = {
  key: string;
  label: string;
  status: string;
};

export type PlatformEpic = {
  epicId: string;
  name: string;
  objective: string;
  releaseStage: string;
  defaultTheme: string;
  sharedDomains: string[];
  surfaces: PlatformEpicSurface[];
  readiness: PlatformEpicChecklist[];
  notificationEvents: string[];
  analyticsEvents: string[];
};
