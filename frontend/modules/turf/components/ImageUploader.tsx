"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Image from "next/image";
import { getImageUrl } from "@/utils/image";

interface ImageUploaderProps {
  onChange: (files: File[]) => void;
  maxFiles?: number;
  existingImages?: string[];
  onExistingChange?: (images: string[]) => void;
  accent?: "player" | "owner";
}

export default function ImageUploader({
  onChange,
  maxFiles = 5,
  existingImages = [],
  onExistingChange,
  accent = "player",
}: ImageUploaderProps) {
  const [files, setFiles] = useState<File[]>([]);
  const [previews, setPreviews] = useState<string[]>([]);
  const previewsRef = useRef<string[]>([]);
  const [dragOver, setDragOver] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    previewsRef.current = previews;
  }, [previews]);

  useEffect(() => {
    return () => {
      previewsRef.current.forEach((url) => URL.revokeObjectURL(url));
    };
  }, []);

  const processFiles = useCallback(
    (incoming: File[]) => {
      const remainingSlots = Math.max(maxFiles - existingImages.length, 0);
      const valid: File[] = [];
      let rejected = false;

      incoming.forEach((file) => {
        const validType = ["image/jpeg", "image/png", "image/webp"].includes(file.type);
        const validSize = file.size <= 5 * 1024 * 1024;
        if (!validType || !validSize) {
          rejected = true;
          return;
        }
        if (valid.length < remainingSlots) {
          valid.push(file);
        } else {
          rejected = true;
        }
      });

      previewsRef.current.forEach((url) => URL.revokeObjectURL(url));
      setFiles(valid);
      setPreviews(valid.map((file) => URL.createObjectURL(file)));
      onChange(valid);

      if (remainingSlots === 0) {
        setMessage(`Maximum ${maxFiles} images already selected. Remove an existing image to add more.`);
      } else if (rejected) {
        setMessage(`Only JPG, PNG, and WEBP images up to 5MB are accepted. Maximum ${maxFiles} images.`);
      } else {
        setMessage("");
      }
    },
    [existingImages.length, maxFiles, onChange]
  );

  const handleDrop = (event: React.DragEvent) => {
    event.preventDefault();
    setDragOver(false);
    processFiles(Array.from(event.dataTransfer.files));
  };

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (!event.target.files) return;
    processFiles(Array.from(event.target.files));
    event.target.value = "";
  };

  const removeImage = (index: number) => {
    const nextFiles = files.filter((_, fileIndex) => fileIndex !== index);
    const nextPreviews = previews.filter((_, previewIndex) => previewIndex !== index);
    URL.revokeObjectURL(previews[index]);
    setFiles(nextFiles);
    setPreviews(nextPreviews);
    onChange(nextFiles);
    setMessage("");
  };

  const removeExistingImage = (image: string) => {
    onExistingChange?.(existingImages.filter((item) => item !== image));
    setMessage("");
  };

  const selectedCount = existingImages.length + files.length;

  return (
    <div className="space-y-3">
      <label className="theme-muted block text-sm font-semibold">
        Turf Images <span className="theme-soft">(max {maxFiles})</span>
      </label>

      <label
        onDrop={handleDrop}
        onDragOver={(event) => {
          event.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        className={`flex h-36 w-full cursor-pointer flex-col items-center justify-center rounded-2xl border-2 border-dashed transition ${
          dragOver
            ? accent === "owner"
              ? "border-sky-400 bg-sky-500/10"
              : "border-emerald-400 bg-emerald-500/10"
            : accent === "owner"
              ? "theme-card hover:border-sky-500/50 hover:bg-[var(--theme-panel-soft)]"
              : "theme-card hover:border-emerald-500/50 hover:bg-[var(--theme-panel-soft)]"
        }`}
      >
        <div className="pointer-events-none text-center">
          <div
            className={`mx-auto mb-2 grid h-9 w-9 place-items-center rounded-xl border text-xs font-black ${
              accent === "owner"
                ? "border-sky-500/30 bg-sky-500/10 text-sky-500"
                : "border-emerald-500/30 bg-emerald-500/10 text-emerald-500"
            }`}
          >
            IMG
          </div>
          <p className="theme-muted text-sm">
            Drag and drop images, or{" "}
            <span className={`font-semibold ${accent === "owner" ? "text-sky-500" : "text-emerald-500"}`}>
              browse
            </span>
          </p>
          <p className="theme-soft mt-1 text-xs">
            JPG, PNG, WEBP up to 5MB each - {selectedCount}/{maxFiles} selected
          </p>
        </div>
        <input
          type="file"
          multiple
          accept="image/jpeg,image/png,image/webp"
          className="hidden"
          onChange={handleChange}
        />
      </label>

      {message && <p className="text-xs font-semibold text-amber-500">{message}</p>}

      {(existingImages.length > 0 || previews.length > 0) && (
        <div className="flex flex-wrap gap-3">
          {existingImages.map((src) => (
            <div key={src} className="theme-card group relative h-20 w-20 overflow-hidden rounded-xl">
              <Image src={getImageUrl(src)} alt="Existing turf image" fill className="object-cover" unoptimized />
              {onExistingChange && (
                <button
                  type="button"
                  aria-label="Remove existing turf image"
                  onClick={() => removeExistingImage(src)}
                  className="absolute inset-0 flex items-center justify-center bg-black/60 text-sm font-black text-white opacity-0 transition group-hover:opacity-100"
                >
                  Remove
                </button>
              )}
            </div>
          ))}
          {previews.map((src, index) => (
            <div key={src} className="theme-card group relative h-20 w-20 overflow-hidden rounded-xl">
              <Image src={src} alt={`New turf image ${index + 1}`} fill className="object-cover" unoptimized />
              <button
                type="button"
                aria-label="Remove selected turf image"
                onClick={() => removeImage(index)}
                className="absolute inset-0 flex items-center justify-center bg-black/60 text-sm font-black text-white opacity-0 transition group-hover:opacity-100"
              >
                Remove
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
