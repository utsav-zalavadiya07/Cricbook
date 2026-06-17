import api from "@/services/api";

export type MediaAsset = {
  id?: number;
  ownerType: "USER" | "OWNER" | "TEAM";
  ownerId: number;
  mediaType: "PROFILE_PHOTO" | "BUSINESS_LOGO" | "TEAM_LOGO";
  imageUrl: string;
  thumbnailUrl: string;
  visibility: string;
  moderationStatus: string;
  moderationReason?: string | null;
  contentType: string;
  fileSizeBytes: number;
  uploadedAt?: string | null;
};

function formData(file: File, extras?: Record<string, string>) {
  const data = new FormData();
  data.append("file", file);
  Object.entries(extras || {}).forEach(([key, value]) => data.append(key, value));
  return data;
}

export const getMyProfileMedia = () => api.get<MediaAsset>("/media/profile/me");
export const uploadProfilePhoto = (file: File, visibility = "PUBLIC") =>
  api.post<MediaAsset>("/media/profile", formData(file, { visibility }), { headers: { "Content-Type": "multipart/form-data" } });
export const removeProfilePhoto = () => api.delete<MediaAsset>("/media/profile");

export const uploadOwnerLogo = (file: File) =>
  api.post<MediaAsset>("/media/owner/logo", formData(file), { headers: { "Content-Type": "multipart/form-data" } });
export const removeOwnerLogo = () => api.delete<MediaAsset>("/media/owner/logo");

export const uploadTeamLogo = (teamId: number, file: File) =>
  api.post<MediaAsset>(`/media/teams/${teamId}/logo`, formData(file), { headers: { "Content-Type": "multipart/form-data" } });
export const removeTeamLogo = (teamId: number) => api.delete<MediaAsset>(`/media/teams/${teamId}/logo`);
