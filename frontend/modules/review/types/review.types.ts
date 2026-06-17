export interface Review {
  id: number;
  userId: number;
  userName?: string;
  turfId: number;
  turfName?: string;
  rating: number;
  title?: string;
  comment: string;
  status?: "PUBLISHED" | "FLAGGED" | "HIDDEN" | "DELETED" | string;
  moderationReason?: string;
  verifiedBooking?: boolean;
  ownerReply?: string;
  ownerReplyAt?: string;
  helpfulCount?: number;
  notHelpfulCount?: number;
  createdAt: string;
  updatedAt?: string;
}

export interface ReviewRequest {
  turfId: number;
  rating: number;
  title?: string;
  comment: string;
}

export interface ReviewSummary {
  turfId: number;
  averageRating: number;
  totalReviews: number;
  ratingBreakdown: Record<number, number>;
  reputationScore: number;
  ownerResponseRate: number;
  flaggedReviews: number;
  reputationLabel: string;
}
