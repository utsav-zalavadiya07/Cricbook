"use client";

import { LocaleCode } from "./messages";

const runtimeCache = new Map<string, string>();
const protectedTokens = [
  { pattern: /cricbook/gi, placeholder: "__CRICBOOK__" },
  { pattern: /\bCB\b/g, placeholder: "__CB__" },
];
const localeScriptMatchers: Record<LocaleCode, RegExp> = {
  en: /\p{Script=Latin}/u,
  hi: /\p{Script=Devanagari}|\p{Script=Latin}/u,
  gu: /\p{Script=Gujarati}|\p{Script=Latin}/u,
  mr: /\p{Script=Devanagari}|\p{Script=Latin}/u,
  bn: /\p{Script=Bengali}|\p{Script=Latin}/u,
  ta: /\p{Script=Tamil}|\p{Script=Latin}/u,
  te: /\p{Script=Telugu}|\p{Script=Latin}/u,
  kn: /\p{Script=Kannada}|\p{Script=Latin}/u,
  ml: /\p{Script=Malayalam}|\p{Script=Latin}/u,
  pa: /\p{Script=Gurmukhi}|\p{Script=Latin}/u,
};
const nonTranslatableTokens = new Set([
  "cricbook",
  "cb",
  "ahmedabad",
  "mumbai",
  "dubai",
  "london",
  "singapore",
]);

function cacheKey(locale: LocaleCode, input: string) {
  return `${locale}::${input}`;
}

export function isLocaleTextCompatible(locale: LocaleCode, input: string) {
  const trimmed = input.trim();
  if (!trimmed) {
    return true;
  }

  const letterChars = [...trimmed].filter((char) => /\p{L}/u.test(char));
  if (letterChars.length === 0) {
    return true;
  }

  const allowedMatcher = localeScriptMatchers[locale];
  return letterChars.every((char) => allowedMatcher.test(char));
}

export function shouldRuntimeTranslate(input: string) {
  const trimmed = input.trim();
  if (!trimmed) {
    return false;
  }

  const lowered = trimmed.toLowerCase();
  if (nonTranslatableTokens.has(lowered)) {
    return false;
  }

  if (/^(https?:\/\/|www\.|\/)/i.test(trimmed)) {
    return false;
  }

  if (/^[\d\s\-–—:/.%#,+()]+$/.test(trimmed)) {
    return false;
  }

  if (/^[A-Z0-9_#-]+$/.test(trimmed)) {
    return false;
  }

  if (/^[\w.-]+@[\w.-]+\.\w+$/.test(trimmed)) {
    return false;
  }

  return /\p{L}/u.test(trimmed);
}

async function fetchFreeTranslation(locale: LocaleCode, input: string) {
  const protectedInput = protectedTokens.reduce(
    (value, token) => value.replace(token.pattern, token.placeholder),
    input
  );
  const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=${locale}&dt=t&q=${encodeURIComponent(protectedInput)}`;
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Free translation failed with ${response.status}`);
  }

  const payload = (await response.json()) as unknown[];
  const parts = Array.isArray(payload?.[0]) ? (payload[0] as unknown[]) : [];
  return protectedTokens.reduce((value, token) => {
    const restored = token.placeholder === "__CRICBOOK__" ? "Cricbook" : "CB";
    return value.replaceAll(token.placeholder, restored);
  }, parts
    .map((part) => (Array.isArray(part) ? String(part[0] || "") : ""))
    .join("")
    .trim());
}

export async function translateRuntimeText(locale: LocaleCode, input: string) {
  if (locale === "en" || !shouldRuntimeTranslate(input)) {
    return input;
  }

  const key = cacheKey(locale, input);
  const cached = runtimeCache.get(key);
  if (cached) {
    return cached;
  }

  const translated = await fetchFreeTranslation(locale, input);
  const safeTranslation = translated && isLocaleTextCompatible(locale, translated) ? translated : input;
  runtimeCache.set(key, safeTranslation);
  return safeTranslation;
}

export async function translateRuntimeBatch(locale: LocaleCode, inputs: string[]) {
  const unique = [...new Set(inputs.filter((input) => shouldRuntimeTranslate(input)))];
  const results = await Promise.all(
    unique.map(async (input) => {
      try {
        return [input, await translateRuntimeText(locale, input)] as const;
      } catch {
        return [input, input] as const;
      }
    })
  );

  return Object.fromEntries(results);
}
