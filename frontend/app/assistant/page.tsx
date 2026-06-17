"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import api from "@/services/api";

type ChatMessage = {
  id: string;
  sender: "user" | "assistant";
  text: string;
  action?: string;
  checkoutLink?: string;
  insightsLink?: string;
};

export default function AIAssistantPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: "init",
      sender: "assistant",
      text: "Hello. I am your Cricbook intelligent assistant.\n\nAsk me to book a slot, find a turf, or analyze your playing and booking patterns.",
    },
  ]);
  const [inputText, setInputText] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const chatEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isTyping]);

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim()) return;

    const userMsg: ChatMessage = {
      id: crypto.randomUUID(),
      sender: "user",
      text: inputText,
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputText("");
    setIsTyping(true);

    try {
      const res = await api.post("/ai/assistant/chat", { message: userMsg.text });
      const data = res.data;
      setMessages((prev) => [
        ...prev,
        {
          id: crypto.randomUUID(),
          sender: "assistant",
          text: data.reply,
          action: data.action,
          checkoutLink: data.checkoutLink,
          insightsLink: data.insightsLink,
        },
      ]);
    } catch (err) {
      console.error(err);
      setMessages((prev) => [
        ...prev,
        {
          id: crypto.randomUUID(),
          sender: "assistant",
          text: "I had trouble processing that request. Please check your login and try again.",
        },
      ]);
    } finally {
      setIsTyping(false);
    }
  };

  return (
    <main className="theme-page-player flex min-h-screen px-4 py-8 sm:px-8">
      <div className="theme-panel mx-auto flex w-full max-w-4xl flex-1 flex-col overflow-hidden rounded-[24px]">
        <div className="theme-panel-soft flex items-center justify-between border-b border-white/10 px-6 py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-emerald-500 to-sky-500 text-sm font-black text-white">
              AI
            </div>
            <div>
              <h1 className="theme-title text-lg font-bold">Cricbook Chat AI</h1>
              <p className="text-xs font-medium text-emerald-700 dark:text-emerald-400">Available now / NLP Engine v2.1</p>
            </div>
          </div>
          <Link href="/personalized" className="theme-card rounded-xl px-3.5 py-2 text-xs font-semibold theme-muted transition hover:border-emerald-400/40 hover:text-current">
            Back to Dashboard
          </Link>
        </div>

        <div className="max-h-[62vh] flex-1 space-y-6 overflow-y-auto p-6">
          {messages.map((msg) => (
            <div key={msg.id} className={`flex ${msg.sender === "user" ? "justify-end" : "justify-start"}`}>
              <div
                className={`max-w-xl rounded-2xl p-4 shadow-sm ${
                  msg.sender === "user"
                    ? "rounded-br-none bg-sky-600 text-white"
                    : "theme-card-strong rounded-bl-none theme-text"
                }`}
              >
                <p className="whitespace-pre-line text-sm leading-relaxed">{msg.text}</p>

                {msg.action === "CHECKOUT" && msg.checkoutLink && (
                  <div className="mt-4 flex flex-col gap-2 border-t border-white/10 pt-3">
                    <span className="text-[11px] font-bold uppercase tracking-wider text-emerald-700 dark:text-emerald-300">Direct checkout prepared</span>
                    <Link href={msg.checkoutLink} className="inline-flex items-center justify-center rounded-xl bg-emerald-500 px-4 py-2.5 text-xs font-bold text-white shadow transition hover:bg-emerald-400">
                      Confirm and Book Slot
                    </Link>
                  </div>
                )}

                {msg.action === "INSIGHTS" && msg.insightsLink && (
                  <div className="mt-4 border-t border-white/10 pt-3">
                    <Link href={msg.insightsLink} className="inline-flex items-center justify-center rounded-xl bg-sky-500 px-4 py-2 text-xs font-bold text-white transition hover:bg-sky-400">
                      View Owner Insights
                    </Link>
                  </div>
                )}

                {msg.action === "RECOMMENDATION" && msg.insightsLink && (
                  <div className="mt-4 border-t border-white/10 pt-3">
                    <Link href={msg.insightsLink} className="inline-flex items-center justify-center rounded-xl bg-emerald-500 px-4 py-2 text-xs font-bold text-white transition hover:bg-emerald-400">
                      Manage Dynamic Rules
                    </Link>
                  </div>
                )}
              </div>
            </div>
          ))}

          {isTyping && (
            <div className="flex justify-start">
              <div className="theme-card flex items-center space-x-1.5 rounded-2xl rounded-bl-none p-4">
                <span className="block h-2 w-2 animate-bounce rounded-full bg-slate-400" />
                <span className="block h-2 w-2 animate-bounce rounded-full bg-slate-400 [animation-delay:0.2s]" />
                <span className="block h-2 w-2 animate-bounce rounded-full bg-slate-400 [animation-delay:0.4s]" />
              </div>
            </div>
          )}
          <div ref={chatEndRef} />
        </div>

        <form onSubmit={handleSendMessage} className="theme-panel-soft flex gap-3 border-t border-white/10 p-4">
          <input
            type="text"
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            placeholder="Type a message or slot request..."
            className="theme-input flex-1 rounded-xl px-4 py-3 text-sm outline-none focus:border-sky-500 focus:ring-2 focus:ring-sky-500/10"
          />
          <button type="submit" className="rounded-xl bg-sky-600 px-6 py-3 font-semibold text-white shadow transition hover:bg-sky-500">
            Send
          </button>
        </form>
      </div>
    </main>
  );
}
