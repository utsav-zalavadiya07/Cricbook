"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { useAuthStore } from "@/store/authStore";
import { phraseAliases, normalizePhrase } from "./dynamic";
import { dictionaries, DEFAULT_LOCALE, LocaleCode, normalizeLocale, TranslationDictionary } from "./messages";
import { isLocaleTextCompatible } from "./freeRuntimeTranslate";
import { getGlobalTranslations, getLanguagePreference, updateLanguagePreference } from "./services/i18nService";

type TranslateValues = Record<string, string | number>;
type LocaleSource = "manual" | "region" | "server";

type I18nContextValue = {
  locale: LocaleCode;
  setLocale: (locale: LocaleCode, options?: { source?: LocaleSource }) => void;
  t: (key: string, values?: TranslateValues) => string;
  translateText: (input: string, values?: TranslateValues) => string;
};

const I18nContext = createContext<I18nContextValue | null>(null);
const LANGUAGE_MODE_KEY = "preferredLanguageMode";

function interpolate(template: string, values?: TranslateValues) {
  if (!values) {
    return template;
  }

  return template.replace(/\{(\w+)\}/g, (_, token) => String(values[token] ?? `{${token}}`));
}

function getStoredLocale() {
  if (typeof window === "undefined") {
    return DEFAULT_LOCALE;
  }

  return normalizeLocale(localStorage.getItem("preferredLanguage") || localStorage.getItem("globalLang"));
}

function getStoredLocaleMode() {
  if (typeof window === "undefined") {
    return null;
  }

  return localStorage.getItem(LANGUAGE_MODE_KEY);
}

function storeLocaleMode(mode: LocaleSource) {
  if (typeof window === "undefined") {
    return;
  }

  localStorage.setItem(LANGUAGE_MODE_KEY, mode);
}

function sanitizeRemoteMessages(locale: LocaleCode, messages: TranslationDictionary) {
  return Object.fromEntries(
    Object.entries(messages).filter(([, value]) => typeof value === "string" && isLocaleTextCompatible(locale, value))
  );
}

export function I18nProvider({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((state) => state.token);
  const [locale, setLocaleState] = useState<LocaleCode>(DEFAULT_LOCALE);
  const [extraMessages, setExtraMessages] = useState<TranslationDictionary>({});
  const [localeOverlayMessages, setLocaleOverlayMessages] = useState<TranslationDictionary>({});
  const [remoteMessages, setRemoteMessages] = useState<TranslationDictionary>({});
  const localeModeRef = useRef<string | null>(null);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const storedLocale = getStoredLocale();
      const storedMode = getStoredLocaleMode();
      localeModeRef.current = storedMode;
      setLocaleState((current) => (current === storedLocale ? current : storedLocale));
    }, 0);

    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }

    document.documentElement.lang = locale;
    localStorage.setItem("preferredLanguage", locale);
    localStorage.setItem("globalLang", locale);
  }, [locale]);

  useEffect(() => {
    let active = true;

    import("./extraMessages")
      .then(({ getExtraMessages }) => {
        if (active) {
          setExtraMessages(getExtraMessages(locale));
        }
      })
      .catch(() => {
        if (active) {
          setExtraMessages({});
        }
      });

    return () => {
      active = false;
    };
  }, [locale]);

  useEffect(() => {
    let active = true;

    if (locale === "hi") {
      import("./hindiOverlay")
        .then(({ getHindiOverlay }) => {
          if (active) {
            setLocaleOverlayMessages(getHindiOverlay(locale));
          }
        })
        .catch(() => {
          if (active) {
            setLocaleOverlayMessages({});
          }
        });
    } else if (locale === "gu") {
      import("./gujaratiOverlay")
        .then(({ getGujaratiOverlay }) => {
          if (active) {
            setLocaleOverlayMessages(getGujaratiOverlay(locale));
          }
        })
        .catch(() => {
          if (active) {
            setLocaleOverlayMessages({});
          }
        });
    } else {
      Promise.resolve().then(() => {
        if (active) {
          setLocaleOverlayMessages({});
        }
      });
    }

    return () => {
      active = false;
    };
  }, [locale]);

  useEffect(() => {
    let active = true;

    getGlobalTranslations(locale)
      .then((messages) => {
        if (active) {
          setRemoteMessages(sanitizeRemoteMessages(locale, messages));
        }
      })
      .catch(() => {
        if (active) {
          setRemoteMessages({});
        }
      });

    return () => {
      active = false;
    };
  }, [locale]);

  useEffect(() => {
    if (!token) {
      return;
    }

    let active = true;

    getLanguagePreference()
      .then((payload) => {
        if (!active) {
          return;
        }

        if (localeModeRef.current === "manual") {
          return;
        }

        const nextLocale = normalizeLocale(payload.preferredLanguage);
        setLocaleState(nextLocale);
        localeModeRef.current = "server";
        storeLocaleMode("server");
      })
      .catch(() => undefined);

    return () => {
      active = false;
    };
  }, [token]);

  const setLocale = useCallback((nextLocale: LocaleCode, options?: { source?: LocaleSource }) => {
    const source = options?.source || "manual";
    if (source === "region" && localeModeRef.current === "manual") {
      return;
    }

    setLocaleState(nextLocale);
    localeModeRef.current = source;
    storeLocaleMode(source);
    if (token && source === "manual") {
      updateLanguagePreference(nextLocale).catch(() => undefined);
    }
  }, [token]);

  const mergedMessages = useMemo(
    () => ({
      ...dictionaries.en,
      ...dictionaries[locale],
      ...extraMessages,
      ...remoteMessages,
      ...localeOverlayMessages,
    }),
    [extraMessages, locale, localeOverlayMessages, remoteMessages]
  );

  const value = useMemo<I18nContextValue>(
    () => ({
      locale,
      setLocale,
      t: (key, values) => interpolate(mergedMessages[key] || dictionaries.en[key] || key, values),
      translateText: (input, values) => {
        const direct = mergedMessages[input];
        if (direct) {
          return interpolate(direct, values);
        }

        const aliasKey = phraseAliases[normalizePhrase(input)];
        if (aliasKey) {
          return interpolate(mergedMessages[aliasKey] || aliasKey, values);
        }

        return interpolate(input, values);
      },
    }),
    [locale, mergedMessages, setLocale]
  );

  return (
    <I18nContext.Provider value={value}>
      <div key={locale} className="contents">
        {children}
      </div>
    </I18nContext.Provider>
  );
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error("useI18n must be used within I18nProvider");
  }
  return context;
}
