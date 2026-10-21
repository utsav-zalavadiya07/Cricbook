"use client";

import { useRef, useState, useEffect } from "react";
import { uploadProfilePhoto, removeProfilePhoto } from "@/modules/media/services/mediaService";
import { updateProfile } from "../services/profileService";
import { getImageUrl } from "@/utils/image";

type EditableProfileUser = {
  name?: string;
  phone?: string;
  email?: string;
  city?: string;
  bio?: string;
  designation?: string;
  alternatePhone?: string;
  businessName?: string;
  businessType?: string;
  gstin?: string;
  address?: string;
  supportEmail?: string;
  photoUrl?: string;
  profileCompletionPercentage?: number;
};

type EditProfileFormProps = {
  user: EditableProfileUser;
  refresh: () => void | Promise<void>;
  accent?: "emerald" | "sky";
  role?: string;
};

const BUSINESS_TYPE_OPTIONS = [
  "Individual / Sole Proprietor",
  "Partnership Firm",
  "Limited Liability Partnership (LLP)",
  "Private Limited Company",
  "Sports Club / Society / Trust",
];

function apiErrorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string } } };
  return apiError.response?.data?.message || fallback;
}

export default function EditProfileForm({ user, refresh, accent = "sky", role }: EditProfileFormProps) {
  const isOwner = role === "OWNER" || accent === "sky";
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [form, setForm] = useState({
    name: user.name || "",
    phone: user.phone || "",
    city: user.city || "",
    bio: user.bio || "",
    designation: user.designation || "",
    alternatePhone: user.alternatePhone || "",
    businessName: user.businessName || "",
    businessType: user.businessType || "Individual / Sole Proprietor",
    gstin: user.gstin || "",
    address: user.address || "",
    supportEmail: user.supportEmail || "",
    photoUrl: user.photoUrl || "",
  });

  const [loading, setLoading] = useState(false);
  const [photoUploading, setPhotoUploading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    setForm({
      name: user.name || "",
      phone: user.phone || "",
      city: user.city || "",
      bio: user.bio || "",
      designation: user.designation || "",
      alternatePhone: user.alternatePhone || "",
      businessName: user.businessName || "",
      businessType: user.businessType || "Individual / Sole Proprietor",
      gstin: user.gstin || "",
      address: user.address || "",
      supportEmail: user.supportEmail || "",
      photoUrl: user.photoUrl || "",
    });
  }, [
    user.name,
    user.phone,
    user.city,
    user.bio,
    user.designation,
    user.alternatePhone,
    user.businessName,
    user.businessType,
    user.gstin,
    user.address,
    user.supportEmail,
    user.photoUrl,
  ]);

  // Profile completion score
  const completion = user.profileCompletionPercentage ?? Math.round(
    ([
      form.name,
      user.email,
      form.phone,
      form.photoUrl,
      form.city,
      form.bio,
      isOwner ? form.businessName || form.designation : true,
    ].filter(Boolean).length / (isOwner ? 7 : 6)) * 100
  );

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setLoading(true);
    setMessage("");
    setError("");
    try {
      await updateProfile({
        name: form.name,
        phone: form.phone,
        city: form.city,
        bio: form.bio,
        designation: form.designation,
        alternatePhone: form.alternatePhone,
        businessName: form.businessName,
        businessType: form.businessType,
        gstin: form.gstin,
        address: form.address,
        supportEmail: form.supportEmail,
      });
      await refresh();
      setMessage("Personal & business details saved successfully!");
      setTimeout(() => setMessage(""), 3500);
    } catch (err) {
      setError(apiErrorMessage(err, "Failed to update profile details."));
    } finally {
      setLoading(false);
    }
  };

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
      setError("Please select a JPG, PNG, or WEBP image.");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError("Image must be 5 MB or smaller.");
      return;
    }

    setPhotoUploading(true);
    setError("");
    setMessage("");
    try {
      const response = await uploadProfilePhoto(file);
      const newUrl = response.data.imageUrl;
      setForm((prev) => ({ ...prev, photoUrl: newUrl }));
      window.dispatchEvent(new CustomEvent("cricbookMediaUpdated", { detail: response.data }));
      await refresh();
      setMessage("Profile photo updated successfully!");
      setTimeout(() => setMessage(""), 3500);
    } catch {
      setError("Failed to upload photo.");
    } finally {
      setPhotoUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handlePhotoRemove = async () => {
    setPhotoUploading(true);
    setError("");
    setMessage("");
    try {
      await removeProfilePhoto();
      setForm((prev) => ({ ...prev, photoUrl: "" }));
      window.dispatchEvent(new CustomEvent("cricbookMediaUpdated", { detail: null }));
      await refresh();
      setMessage("Profile photo removed.");
      setTimeout(() => setMessage(""), 3500);
    } catch {
      setError("Failed to remove photo.");
    } finally {
      setPhotoUploading(false);
    }
  };

  const inputCls = `theme-input w-full rounded-xl px-4 py-3 text-xs sm:text-sm font-semibold outline-none transition ${
    accent === "sky"
      ? "focus:border-sky-500 focus:ring-1 focus:ring-sky-500"
      : "focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500"
  }`;

  const currentDisplayPhoto = getImageUrl(form.photoUrl);

  return (
    <div className="theme-panel overflow-hidden rounded-3xl border border-[var(--theme-border)] shadow-xl">
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="hidden"
        onChange={handlePhotoUpload}
      />

      {/* Header with Title and Profile Completion Meter */}
      <div className="flex flex-col gap-4 border-b border-[var(--theme-border)] p-5 sm:flex-row sm:items-center sm:justify-between sm:p-7">
        <div>
          <h3 className="theme-title text-xl font-black">
            {isOwner ? "Personal & Business Details" : "Personal Details"}
          </h3>
          <p className="theme-muted mt-1 text-xs sm:text-sm">
            {isOwner
              ? "Official owner contact, business registration, and customer verification details."
              : "This information will be visible to friends and turf hosts on your bookings."}
          </p>
        </div>

        {/* Profile Completion Meter */}
        <div className="sm:text-right shrink-0">
          <div className="flex items-center gap-2 sm:justify-end">
            <span className="theme-muted text-xs font-semibold">Profile completion</span>
            <span className={`text-sm font-black ${accent === "sky" ? "text-sky-400" : "text-emerald-400"}`}>
              {completion}%
            </span>
          </div>
          <div className="mt-1.5 h-2 w-48 max-w-full overflow-hidden rounded-full bg-white/10">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                accent === "sky"
                  ? "bg-gradient-to-r from-sky-500 to-blue-500 shadow-sm shadow-sky-500/50"
                  : "bg-gradient-to-r from-emerald-500 to-teal-500 shadow-sm shadow-emerald-500/50"
              }`}
              style={{ width: `${completion}%` }}
            />
          </div>
        </div>
      </div>

      <div className="space-y-6 p-5 sm:p-7">
        {/* Photo Management Row */}
        <div className="theme-panel-soft flex flex-col gap-4 rounded-2xl border border-[var(--theme-border)] p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3.5">
            <div className="relative flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-xl border border-[var(--theme-border)] bg-slate-900 shadow-inner">
              {currentDisplayPhoto ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={currentDisplayPhoto} alt="Profile" className="h-full w-full object-cover" />
              ) : (
                <svg className="h-6 w-6 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
              )}
            </div>
            <div>
              <p className="theme-title text-sm font-bold">Profile photo</p>
              <p className="theme-muted text-xs">
                Upload a clear photo for your profile. JPG, PNG, WEBP (Max 5 MB)
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 sm:shrink-0">
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={photoUploading}
              className={`rounded-xl px-4 py-2 text-xs font-bold text-white shadow-sm transition ${
                accent === "sky" ? "bg-sky-500 hover:bg-sky-400" : "bg-emerald-500 hover:bg-emerald-400"
              } disabled:opacity-50`}
            >
              {photoUploading ? "Uploading..." : "Upload photo"}
            </button>

            {currentDisplayPhoto && (
              <button
                type="button"
                onClick={handlePhotoRemove}
                disabled={photoUploading}
                className="rounded-xl border border-[var(--theme-border)] bg-white/5 px-4 py-2 text-xs font-bold text-slate-300 transition hover:bg-red-500/10 hover:border-red-500/30 hover:text-red-400 disabled:opacity-50"
              >
                Remove
              </button>
            )}
          </div>
        </div>

        {/* Feedback Alerts */}
        {message && (
          <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm font-semibold text-emerald-400 animate-in fade-in">
            {message}
          </div>
        )}
        {error && (
          <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm font-semibold text-red-400 animate-in fade-in">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* SECTION 1: Personal & Executive Contact Details */}
          <div className="rounded-2xl border border-[var(--theme-border)] bg-black/10 p-5 space-y-4">
            <div className="border-b border-[var(--theme-border)] pb-3">
              <h4 className="theme-title font-black text-sm uppercase tracking-wider flex items-center gap-2">
                <span className={`h-2 w-2 rounded-full ${accent === "sky" ? "bg-sky-400" : "bg-emerald-400"}`} />
                <span>Personal & Contact Information</span>
              </h4>
              <p className="theme-muted text-xs mt-0.5">Primary owner identity and communication details.</p>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              {/* Full Name */}
              <div>
                <label className="theme-muted mb-1.5 flex items-center gap-1 text-xs font-bold uppercase tracking-wider">
                  <span>Full name</span>
                  <span className="text-red-400">*</span>
                </label>
                <input
                  className={inputCls}
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  placeholder="Enter full legal name"
                  autoComplete="name"
                  required
                />
              </div>

              {/* Designation / Role */}
              <div>
                <label className="theme-muted mb-1.5 flex items-center gap-1 text-xs font-bold uppercase tracking-wider">
                  <span>Designation / Role</span>
                </label>
                <input
                  className={inputCls}
                  value={form.designation}
                  onChange={(e) => setForm({ ...form, designation: e.target.value })}
                  placeholder="e.g. Venue Owner, Managing Partner, Operations Head"
                />
              </div>

              {/* Primary Phone */}
              <div>
                <label className="theme-muted mb-1.5 flex items-center gap-1 text-xs font-bold uppercase tracking-wider">
                  <span>Primary Phone Number</span>
                  <span className="text-red-400">*</span>
                </label>
                <input
                  className={inputCls}
                  value={form.phone}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })}
                  placeholder="e.g. 9825012345"
                  autoComplete="tel"
                  required
                />
                <span className="theme-soft text-[11px] mt-1 block">Used for primary account login and critical OTPs.</span>
              </div>

              {/* Alternate Phone */}
              <div>
                <label className="theme-muted mb-1.5 flex items-center gap-1 text-xs font-bold uppercase tracking-wider">
                  <span>Alternate / Emergency Phone</span>
                </label>
                <input
                  className={inputCls}
                  value={form.alternatePhone}
                  onChange={(e) => setForm({ ...form, alternatePhone: e.target.value })}
                  placeholder="e.g. +91 9825244321"
                />
                <span className="theme-soft text-[11px] mt-1 block">Secondary contact for match day or player emergencies.</span>
              </div>

              {/* Operating City */}
              <div>
                <label className="theme-muted mb-1.5 flex items-center gap-1 text-xs font-bold uppercase tracking-wider">
                  <span>Operating City / Region</span>
                </label>
                <input
                  className={inputCls}
                  value={form.city}
                  onChange={(e) => setForm({ ...form, city: e.target.value })}
                  placeholder="e.g. Surat, Gujarat"
                />
              </div>

              {/* Email Address (Locked) */}
              <div>
                <label className="theme-muted mb-1.5 flex items-center gap-1 text-xs font-bold uppercase tracking-wider">
                  <span>Account Email</span>
                </label>
                <div className="relative">
                  <input
                    className={`${inputCls} cursor-not-allowed opacity-75 pr-10`}
                    value={user.email || ""}
                    disabled
                    readOnly
                  />
                  <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                  </span>
                </div>
                <span className="theme-soft text-[11px] mt-1 block">Managed through account security settings.</span>
              </div>
            </div>
          </div>

          {/* SECTION 2: Business & Legal Entity Profile (for Owner portal) */}
          {isOwner && (
            <div className="rounded-2xl border border-[var(--theme-border)] bg-black/10 p-5 space-y-4">
              <div className="border-b border-[var(--theme-border)] pb-3">
                <h4 className="theme-title font-black text-sm uppercase tracking-wider flex items-center gap-2">
                  <span className={`h-2 w-2 rounded-full ${accent === "sky" ? "bg-sky-400" : "bg-emerald-400"}`} />
                  <span>Business & Entity Details</span>
                </h4>
                <p className="theme-muted text-xs mt-0.5">Commercial registration and tax information for invoicing and compliance.</p>
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                {/* Business Name */}
                <div>
                  <label className="theme-muted mb-1.5 flex items-center gap-1 text-xs font-bold uppercase tracking-wider">
                    <span>Business / Legal Entity Name</span>
                  </label>
                  <input
                    className={inputCls}
                    value={form.businessName}
                    onChange={(e) => setForm({ ...form, businessName: e.target.value })}
                    placeholder="e.g. Shree Krishna Sports & Turf Management LLP"
                  />
                  <span className="theme-soft text-[11px] mt-1 block">Printed on official invoices and booking confirmations.</span>
                </div>

                {/* Business Type */}
                <div>
                  <label className="theme-muted mb-1.5 flex items-center gap-1 text-xs font-bold uppercase tracking-wider">
                    <span>Entity Structure / Type</span>
                  </label>
                  <select
                    className={inputCls}
                    value={form.businessType}
                    onChange={(e) => setForm({ ...form, businessType: e.target.value })}
                  >
                    {BUSINESS_TYPE_OPTIONS.map((type) => (
                      <option key={type} value={type} className="bg-slate-900 text-white">
                        {type}
                      </option>
                    ))}
                  </select>
                </div>

                {/* GSTIN / Tax ID */}
                <div>
                  <label className="theme-muted mb-1.5 flex items-center gap-1 text-xs font-bold uppercase tracking-wider">
                    <span>GSTIN / Tax ID / MSME</span>
                  </label>
                  <input
                    className={inputCls}
                    value={form.gstin}
                    onChange={(e) => setForm({ ...form, gstin: e.target.value.toUpperCase() })}
                    placeholder="e.g. 24AAAAA0000A1Z5"
                    maxLength={20}
                  />
                  <span className="theme-soft text-[11px] mt-1 block">Used for GST credit and formal vendor tax invoices.</span>
                </div>

                {/* Registered Address */}
                <div>
                  <label className="theme-muted mb-1.5 flex items-center gap-1 text-xs font-bold uppercase tracking-wider">
                    <span>Facility / Registered Office Address</span>
                  </label>
                  <input
                    className={inputCls}
                    value={form.address}
                    onChange={(e) => setForm({ ...form, address: e.target.value })}
                    placeholder="e.g. Near Ring Road, Pal-Adajan, Surat - 395009"
                  />
                </div>
              </div>
            </div>
          )}

          {/* SECTION 3: Customer Support & Bio */}
          <div className="rounded-2xl border border-[var(--theme-border)] bg-black/10 p-5 space-y-4">
            <div className="border-b border-[var(--theme-border)] pb-3">
              <h4 className="theme-title font-black text-sm uppercase tracking-wider flex items-center gap-2">
                <span className={`h-2 w-2 rounded-full ${accent === "sky" ? "bg-sky-400" : "bg-emerald-400"}`} />
                <span>Customer Inquiries & Bio</span>
              </h4>
              <p className="theme-muted text-xs mt-0.5">Information shared with players and displayed on your booking page.</p>
            </div>

            <div className="space-y-4">
              {/* Customer Support Email */}
              {isOwner && (
                <div>
                  <label className="theme-muted mb-1.5 flex items-center gap-1 text-xs font-bold uppercase tracking-wider">
                    <span>Customer Support Email</span>
                  </label>
                  <input
                    type="email"
                    className={inputCls}
                    value={form.supportEmail}
                    onChange={(e) => setForm({ ...form, supportEmail: e.target.value })}
                    placeholder="e.g. support@krishnaturf.com or bookings@krishnaturf.com"
                  />
                  <span className="theme-soft text-[11px] mt-1 block">Public email displayed to customers for inquiries and disputes.</span>
                </div>
              )}

              {/* Bio / Description */}
              <div>
                <label className="theme-muted mb-1.5 flex items-center gap-1 text-xs font-bold uppercase tracking-wider">
                  <span>{isOwner ? "Owner & Facility Bio" : "About You / Bio"}</span>
                </label>
                <textarea
                  rows={3}
                  className={`${inputCls} resize-none`}
                  value={form.bio}
                  onChange={(e) => setForm({ ...form, bio: e.target.value })}
                  placeholder={
                    isOwner
                      ? "Write a brief intro about your sports facility, experience, amenities, and mission for players..."
                      : "Tell other players about yourself and your cricket journey..."
                  }
                  maxLength={500}
                />
                <div className="flex justify-between items-center text-[11px] mt-1">
                  <span className="theme-soft">A brief bio helps build player trust and credibility.</span>
                  <span className="text-slate-500 font-bold">{form.bio.length}/500</span>
                </div>
              </div>
            </div>
          </div>

          {/* Action Row */}
          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={loading}
              className={`rounded-xl px-8 py-3 text-sm font-bold text-white shadow-lg transition active:scale-95 disabled:opacity-50 ${
                accent === "sky"
                  ? "bg-gradient-to-r from-sky-500 to-blue-600 hover:from-sky-400 hover:to-blue-500 shadow-sky-500/25"
                  : "bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 shadow-emerald-500/25"
              }`}
            >
              {loading ? "Saving changes..." : "Save changes"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
