import api from "@/services/api";
import { Review, ReviewRequest, ReviewSummary } from "../types/review.types";

export const getReviews = (turfId: number) =>
  api.get<Review[]>(`/reviews/turf/${turfId}`);

export const getReviewSummary = (turfId: number) =>
  api.get<ReviewSummary>(`/reviews/turf/${turfId}/summary`);

export const addReview = (data: ReviewRequest) =>
  api.post<Review>("/reviews", data);

export const editReview = (reviewId: number, data: ReviewRequest) =>
  api.put<Review>(`/reviews/${reviewId}`, data);

export const deleteReview = (reviewId: number) =>
  api.delete(`/reviews/${reviewId}`);

export const replyToReview = (reviewId: number, reply: string) =>
  api.post<Review>(`/reviews/${reviewId}/owner-reply`, { reply });

export const voteReview = (reviewId: number, helpful: boolean) =>
  api.post<Review>(`/reviews/${reviewId}/vote`, { helpful });

export const reportReview = (reviewId: number, reason: string) =>
  api.post<Review>(`/reviews/${reviewId}/report`, { reason });
