"use client";

import { useState } from "react";
import AvatarUploader from "@/modules/media/components/AvatarUploader";
import { removeProfilePhoto, uploadProfilePhoto } from "@/modules/media/services/mediaService";
import { updateProfile } from "../services/profileService";

type EditableProfileUser = {
  name?: string;
  phone?: string;
  email?: string;
  photoUrl?: string;
};

type EditProfileFormProps = {
  user: EditableProfileUser;
  refresh: () => void | Promise<void>;
};

function apiErrorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string } } };
  return apiError.response?.data?.message || fallback;
}

export default function EditProfileForm({ user, refresh }: EditProfileFormProps) {
  const [form, setForm] = useState({
    name: user.name || "",
    phone: user.phone || "",
    photoUrl: user.photoUrl || "",
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = async () => {
    setLoading(true);
    setMessage("");
    setError("");
    try {
      await updateProfile({ name: form.name, phone: form.phone });
      void refresh();
      setMessage("Profile updated successfully!");
    } catch (err) {
      setError(apiErrorMessage(err, "Failed to update profile."));
    } finally {
      setLoading(false);
    }
  };

  const inputCls = "theme-input w-full rounded-xl px-4 py-3 text-sm outline-none transition focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500";

  return (
    <div className="theme-panel mt-4 space-y-5 rounded-2xl p-6 shadow-lg">
      <h3 className="theme-title text-lg font-semibold">Edit Profile</h3>

      <AvatarUploader
        title="Profile Photo"
        body="Upload a clear photo for your Cricbook identity, teams, bookings, and community activity."
        imageUrl={form.photoUrl}
        fallbackText={form.name || user.email || "CB"}
        onUpload={async (file) => {
          const response = await uploadProfilePhoto(file);
          return response.data;
        }}
        onRemove={async () => {
          const response = await removeProfilePhoto();
          return response.data;
        }}
        onChange={(asset) => {
          const photoUrl = asset.imageUrl.startsWith("data:") ? "" : asset.imageUrl;
          setForm((current) => ({ ...current, photoUrl }));
          void refresh();
        }}
      />
      
      {message && <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-600 dark:text-emerald-300">{message}</div>}
      {error && <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-600 dark:text-red-300">{error}</div>}

      <div>
        <label className="theme-muted mb-1.5 block text-sm font-medium">Full Name</label>
        <input
          className={inputCls}
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
        />
      </div>

      <div>
        <label className="theme-muted mb-1.5 block text-sm font-medium">Phone Number</label>
        <input
          className={inputCls}
          value={form.phone}
          onChange={(e) => setForm({ ...form, phone: e.target.value })}
        />
      </div>

      <button
        onClick={handleSubmit}
        disabled={loading}
        className="mt-2 w-full rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 py-3 font-semibold text-white transition-all duration-200 hover:from-emerald-400 hover:to-teal-500 disabled:opacity-50"
      >
        {loading ? "Saving..." : "Save Changes"}
      </button>
    </div>
  );
}
