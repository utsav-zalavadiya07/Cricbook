import { apiRequest } from "../api/client";
import { MediaAsset } from "../types/models";

type MobileUploadFile = {
  uri: string;
  name: string;
  type: "image/jpeg" | "image/png" | "image/webp" | string;
};

function toFormData(file: MobileUploadFile, extras?: Record<string, string>) {
  const data = new FormData();
  data.append("file", file as unknown as Blob);
  Object.entries(extras || {}).forEach(([key, value]) => data.append(key, value));
  return data;
}

export const getMyProfileMedia = () => apiRequest<MediaAsset>("/media/profile/me");

export const uploadProfilePhoto = (file: MobileUploadFile, visibility = "PUBLIC") =>
  apiRequest<MediaAsset>("/media/profile", {
    method: "POST",
    body: toFormData(file, { visibility }),
  });

export const removeProfilePhoto = () =>
  apiRequest<MediaAsset>("/media/profile", {
    method: "DELETE",
  });

export const uploadOwnerLogo = (file: MobileUploadFile) =>
  apiRequest<MediaAsset>("/media/owner/logo", {
    method: "POST",
    body: toFormData(file),
  });

export const removeOwnerLogo = () =>
  apiRequest<MediaAsset>("/media/owner/logo", {
    method: "DELETE",
  });

export const uploadTeamLogo = (teamId: number, file: MobileUploadFile) =>
  apiRequest<MediaAsset>(`/media/teams/${teamId}/logo`, {
    method: "POST",
    body: toFormData(file),
  });

export const removeTeamLogo = (teamId: number) =>
  apiRequest<MediaAsset>(`/media/teams/${teamId}/logo`, {
    method: "DELETE",
  });
