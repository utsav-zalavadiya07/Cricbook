"use client";

import { useRef, useState } from "react";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { getImageUrl } from "@/utils/image";
import type { MediaAsset } from "../services/mediaService";

type AvatarUploaderProps = {
  title: string;
  body: string;
  imageUrl?: string | null;
  fallbackText: string;
  accent?: "emerald" | "sky";
  onUpload: (file: File) => Promise<MediaAsset>;
  onRemove?: () => Promise<MediaAsset>;
  onChange?: (asset: MediaAsset) => void;
};

const allowedTypes = ["image/jpeg", "image/png", "image/webp"];

export default function AvatarUploader({ title, body, imageUrl, fallbackText, accent = "emerald", onUpload, onRemove, onChange }: AvatarUploaderProps) {
  const { translateText } = useI18n();
  const inputRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  const accentClass =
    accent === "sky"
      ? "from-sky-500 to-blue-700 shadow-sky-500/20 border-sky-400/40 text-sky-600 dark:text-sky-200"
      : "from-emerald-500 to-teal-600 shadow-emerald-500/20 border-emerald-400/40 text-emerald-600 dark:text-emerald-200";

  const handleFile = async (file?: File) => {
    setError("");
    setMessage("");
    if (!file) return;
    if (!allowedTypes.includes(file.type)) {
      setError(translateText("Use JPG, PNG, or WEBP image."));
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError(translateText("Image must be 5 MB or smaller."));
      return;
    }
    const localPreview = URL.createObjectURL(file);
    setPreview(localPreview);
    setBusy(true);
    try {
      const asset = await onUpload(file);
      setPreview(null);
      onChange?.(asset);
      window.dispatchEvent(new CustomEvent("cricbookMediaUpdated", { detail: asset }));
      setMessage(translateText("Image updated."));
    } catch (err) {
      setPreview(null);
      setError(translateText(readApiError(err, "Unable to upload image.")));
    } finally {
      setBusy(false);
      URL.revokeObjectURL(localPreview);
      if (inputRef.current) inputRef.current.value = "";
    }
  };

  const handleRemove = async () => {
    if (!onRemove) return;
    setBusy(true);
    setError("");
    setMessage("");
    try {
      const asset = await onRemove();
      onChange?.(asset);
      window.dispatchEvent(new CustomEvent("cricbookMediaUpdated", { detail: asset }));
      setMessage(translateText("Image removed."));
    } catch (err) {
      setError(translateText(readApiError(err, "Unable to remove image.")));
    } finally {
      setBusy(false);
    }
  };

  const displayUrl = preview || getImageUrl(imageUrl);
  const initials = fallbackText.trim().slice(0, 2).toUpperCase() || "CB";

  return (
    <div className="theme-panel-soft rounded-3xl p-5">
      <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <div className="relative h-24 w-24 shrink-0 overflow-hidden rounded-3xl border border-[var(--theme-border)] bg-[var(--theme-card)] shadow-lg">
            {displayUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={displayUrl} alt={title} className="h-full w-full object-cover" />
            ) : (
              <div className={`flex h-full w-full items-center justify-center bg-gradient-to-br ${accent === "sky" ? "from-sky-500 to-blue-700" : "from-emerald-500 to-teal-700"} text-2xl font-black text-white`}>
                {initials}
              </div>
            )}
          </div>
          <div>
            <p className="theme-title text-lg font-black">{translateText(title)}</p>
            <p className="theme-muted mt-1 max-w-xl text-sm leading-6">{translateText(body)}</p>
            <p className="theme-soft mt-2 text-xs font-semibold">{translateText("JPG, PNG, WEBP. Max 5 MB.")}</p>
          </div>
        </div>

        <div className="flex flex-wrap gap-3">
          <input ref={inputRef} type="file" accept="image/jpeg,image/png,image/webp" className="hidden" onChange={(event) => void handleFile(event.target.files?.[0])} />
          <button
            type="button"
            onClick={() => inputRef.current?.click()}
            disabled={busy}
            className={`rounded-2xl bg-gradient-to-r px-5 py-3 text-sm font-black text-white shadow-lg transition disabled:opacity-60 ${accentClass}`}
          >
            {busy ? translateText("Uploading...") : translateText("Upload")}
          </button>
          {onRemove && imageUrl && (
            <button type="button" onClick={() => void handleRemove()} disabled={busy} className="rounded-2xl border border-red-400/40 px-5 py-3 text-sm font-black text-red-600 transition hover:bg-red-500/10 disabled:opacity-60 dark:text-red-300">
              {translateText("Remove")}
            </button>
          )}
        </div>
      </div>
      {message && <p className={`mt-3 text-sm font-bold ${accent === "sky" ? "text-sky-600 dark:text-sky-300" : "text-emerald-600 dark:text-emerald-300"}`}>{message}</p>}
      {error && <p className="mt-3 text-sm font-bold text-red-600 dark:text-red-300">{error}</p>}
    </div>
  );
}

function readApiError(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string; error?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
