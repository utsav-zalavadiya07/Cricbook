"use client";

import { useEffect } from "react";
import { useI18n } from "./I18nProvider";
import { shouldRuntimeTranslate, translateRuntimeBatch } from "./freeRuntimeTranslate";

const textNodeOriginals = new WeakMap<Text, string>();

type Binding =
  | { type: "text"; node: Text; original: string }
  | { type: "attr"; element: HTMLElement; attr: "placeholder" | "title" | "aria-label"; original: string };

function shouldSkipElement(element: Element | null) {
  if (!element) {
    return true;
  }

  if (element.closest("[data-i18n-skip='true']")) {
    return true;
  }

  const tagName = element.tagName.toLowerCase();
  return ["script", "style", "noscript", "code", "pre"].includes(tagName);
}

function getStoredAttributeOriginal(element: HTMLElement, attr: "placeholder" | "title" | "aria-label") {
  const key =
    attr === "placeholder"
      ? "i18nOriginalPlaceholder"
      : attr === "title"
        ? "i18nOriginalTitle"
        : "i18nOriginalAriaLabel";

  return element.dataset[key];
}

function setStoredAttributeOriginal(element: HTMLElement, attr: "placeholder" | "title" | "aria-label", value: string) {
  const key =
    attr === "placeholder"
      ? "i18nOriginalPlaceholder"
      : attr === "title"
        ? "i18nOriginalTitle"
        : "i18nOriginalAriaLabel";

  element.dataset[key] = value;
}

export default function DomTranslator() {
  const { locale, translateText } = useI18n();

  useEffect(() => {
    if (typeof document === "undefined") {
      return;
    }

    if (locale === "en") {
      // English is the source UI. The keyed locale remount from I18nProvider
      // recreates the original DOM, so replaying stored mutated text here can
      // wrongly restore Hindi/Gujarati content when a page first rendered in
      // another locale.
      return;
    }

    let cancelled = false;
    let frame: number | null = null;

    const processTree = async () => {
      if (cancelled) {
        return;
      }

      const pending: Binding[] = [];
      const runtimeTexts = new Set<string>();

      const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
      let current = walker.nextNode();

      while (current) {
        const textNode = current as Text;
        const parent = textNode.parentElement;
        const rawValue = textNode.nodeValue || "";
        const original = textNodeOriginals.get(textNode) || rawValue;

        if (!textNodeOriginals.has(textNode)) {
          textNodeOriginals.set(textNode, rawValue);
        }

        if (!parent || shouldSkipElement(parent) || parent instanceof HTMLInputElement || parent instanceof HTMLTextAreaElement) {
          current = walker.nextNode();
          continue;
        }

        const trimmed = original.trim();
        if (!trimmed) {
          current = walker.nextNode();
          continue;
        }

        const translated = translateText(trimmed);
        if (translated !== trimmed) {
          textNode.nodeValue = original.replace(trimmed, translated);
        } else if (shouldRuntimeTranslate(trimmed)) {
          pending.push({ type: "text", node: textNode, original });
          runtimeTexts.add(trimmed);
        } else {
          textNode.nodeValue = original;
        }

        current = walker.nextNode();
      }

      const attrElements = document.querySelectorAll<HTMLElement>("[placeholder], [title], [aria-label]");
      attrElements.forEach((element) => {
        if (shouldSkipElement(element)) {
          return;
        }

        (["placeholder", "title", "aria-label"] as const).forEach((attr) => {
          const currentValue = element.getAttribute(attr);
          if (!currentValue) {
            return;
          }

          const original = getStoredAttributeOriginal(element, attr) || currentValue;
          if (!getStoredAttributeOriginal(element, attr)) {
            setStoredAttributeOriginal(element, attr, currentValue);
          }

          const translated = translateText(original);
          if (translated !== original) {
            element.setAttribute(attr, translated);
          } else if (shouldRuntimeTranslate(original)) {
            pending.push({ type: "attr", element, attr, original });
            runtimeTexts.add(original);
          } else {
            element.setAttribute(attr, original);
          }
        });
      });

      if (runtimeTexts.size === 0) {
        return;
      }

      const runtimeMap = await translateRuntimeBatch(locale, [...runtimeTexts]);
      if (cancelled) {
        return;
      }

      pending.forEach((binding) => {
        const translated = runtimeMap[binding.original.trim()] || binding.original;
        if (binding.type === "text") {
          const trimmed = binding.original.trim();
          binding.node.nodeValue = binding.original.replace(trimmed, translated);
          return;
        }

        binding.element.setAttribute(binding.attr, translated);
      });
    };

    const schedule = () => {
      if (frame !== null) {
        cancelAnimationFrame(frame);
      }

      frame = requestAnimationFrame(() => {
        frame = null;
        void processTree();
      });
    };

    schedule();

    const observer = new MutationObserver(() => schedule());
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      characterData: true,
      attributes: true,
      attributeFilter: ["placeholder", "title", "aria-label"],
    });

    return () => {
      cancelled = true;
      observer.disconnect();
      if (frame !== null) {
        cancelAnimationFrame(frame);
      }
    };
  }, [locale, translateText]);

  return null;
}
