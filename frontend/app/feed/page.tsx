"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import {
  addFeedComment,
  createFeedPost,
  deleteFeedPost,
  getFeedComments,
  getSocialFeed,
  reactToFeedPost,
  removeFeedReaction,
  reportContent,
} from "@/modules/social/services/socialService";
import { FeedComment, FeedEvent } from "@/modules/social/types/social.types";

const inputClass =
  "theme-input w-full rounded-2xl px-4 py-3 text-sm outline-none transition focus:border-emerald-400/40 focus:ring-2 focus:ring-emerald-500/10";

export default function FeedPage() {
  const [feed, setFeed] = useState<FeedEvent[]>([]);
  const [comments, setComments] = useState<Record<number, FeedComment[]>>({});
  const [commentDrafts, setCommentDrafts] = useState<Record<number, string>>({});
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");
  const [scope, setScope] = useState("PUBLIC");
  const [sort, setSort] = useState("LATEST");
  const [form, setForm] = useState({ title: "", content: "", visibility: "PUBLIC", mediaUrls: "" });

  const loadFeed = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getSocialFeed({
        scope,
        query: query.trim() || undefined,
        sort,
      });
      setFeed(res.data as FeedEvent[]);
      setStatus("");
    } catch (error) {
      setStatus(errorMessage(error, "Unable to load community feed."));
    } finally {
      setLoading(false);
    }
  }, [query, scope, sort]);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadFeed(), 0);
    return () => window.clearTimeout(timer);
  }, [loadFeed]);

  const submitPost = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    try {
      await createFeedPost({
        title: form.title || "Community update",
        content: form.content,
        visibility: form.visibility,
        mediaUrls: form.mediaUrls.split("\n").map((item) => item.trim()).filter(Boolean),
      });
      setForm({ title: "", content: "", visibility: "PUBLIC", mediaUrls: "" });
      await loadFeed();
    } catch (error) {
      setStatus(errorMessage(error, "Post creation failed."));
    }
  };

  const toggleReaction = async (item: FeedEvent, reactionType: string) => {
    try {
      const res = item.viewerReaction === reactionType
        ? await removeFeedReaction(item.id)
        : await reactToFeedPost(item.id, reactionType);
      setFeed((current) => current.map((post) => (post.id === item.id ? res.data : post)));
    } catch (error) {
      setStatus(errorMessage(error, "Reaction update failed."));
    }
  };

  const loadComments = async (postId: number) => {
    try {
      const res = await getFeedComments(postId);
      setComments((current) => ({ ...current, [postId]: res.data as FeedComment[] }));
    } catch (error) {
      setStatus(errorMessage(error, "Could not load comments."));
    }
  };

  const submitComment = async (postId: number) => {
    const message = commentDrafts[postId]?.trim();
    if (!message) return;
    try {
      await addFeedComment(postId, message);
      setCommentDrafts((current) => ({ ...current, [postId]: "" }));
      await loadComments(postId);
      await loadFeed();
    } catch (error) {
      setStatus(errorMessage(error, "Comment failed."));
    }
  };

  const reportPost = async (postId: number) => {
    try {
      await reportContent("POST", postId, "SPAM", "Reported from community feed.");
      setStatus("Post reported for moderation.");
    } catch (error) {
      setStatus(errorMessage(error, "Report failed."));
    }
  };

  const removePost = async (postId: number) => {
    try {
      await deleteFeedPost(postId);
      setFeed((current) => current.filter((post) => post.id !== postId));
    } catch (error) {
      setStatus(errorMessage(error, "Delete failed."));
    }
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      <div className="w-full space-y-6">
        <section className="theme-panel-strong rounded-[2rem] p-6 shadow-2xl shadow-black/20 sm:p-8">
          <p className="theme-accent-player text-xs font-black uppercase tracking-[0.3em]">EPIC 15 - Community Feed</p>
          <h1 className="theme-title mt-3 text-3xl font-black tracking-tight sm:text-5xl">Community posts, reactions, comments, and moderation.</h1>
          <p className="theme-muted mt-3 max-w-3xl text-sm leading-6">
            Create cricket updates, react to team activity, comment on posts, and report unsafe content from one feed.
          </p>
        </section>

        {status && <p className="theme-card rounded-2xl px-4 py-3 text-sm font-semibold text-emerald-300">{status}</p>}

        <section className="grid gap-6 xl:grid-cols-[420px_1fr]">
          <div className="space-y-4">
            <form onSubmit={submitPost} className="theme-panel rounded-3xl p-5">
              <h2 className="theme-title text-xl font-black">Create post</h2>
              <p className="theme-muted mt-1 text-sm">Text, emojis, and media URLs are supported. Team-only posts can be added from team context later.</p>
              <div className="mt-4 space-y-3">
                <input value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="Post title" className={inputClass} />
                <textarea value={form.content} onChange={(event) => setForm({ ...form, content: event.target.value })} placeholder="What happened in cricket today?" className={`${inputClass} min-h-32`} maxLength={1000} />
                <select value={form.visibility} onChange={(event) => setForm({ ...form, visibility: event.target.value })} className={inputClass}>
                  <option value="PUBLIC">Public</option>
                  <option value="FOLLOWERS">Followers only</option>
                  <option value="PRIVATE">Private</option>
                </select>
                <textarea value={form.mediaUrls} onChange={(event) => setForm({ ...form, mediaUrls: event.target.value })} placeholder="Optional media URLs, one per line" className={`${inputClass} min-h-24`} />
                <button className="w-full rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white shadow-lg shadow-emerald-950/20">
                  Publish post
                </button>
              </div>
            </form>

            <div className="theme-panel rounded-3xl p-5">
              <h2 className="theme-title text-xl font-black">Find posts</h2>
              <div className="mt-4 space-y-3">
                <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search posts, hashtags, teams" className={inputClass} />
                <select value={scope} onChange={(event) => setScope(event.target.value)} className={inputClass}>
                  <option value="PUBLIC">Public</option>
                  <option value="FOLLOWERS">Followers</option>
                  <option value="TEAM">Team</option>
                </select>
                <select value={sort} onChange={(event) => setSort(event.target.value)} className={inputClass}>
                  <option value="LATEST">Latest</option>
                  <option value="TRENDING">Trending</option>
                </select>
                <button type="button" onClick={loadFeed} className="w-full rounded-2xl border border-emerald-400/30 px-4 py-3 text-sm font-black text-emerald-700 dark:text-emerald-200">
                  Refresh feed
                </button>
              </div>
            </div>
          </div>

          <div className="space-y-4">
            {loading && <div className="theme-card rounded-3xl p-10 text-center text-sm theme-muted">Loading feed...</div>}
            {!loading && feed.length === 0 && <div className="theme-card theme-muted rounded-3xl border-dashed p-10 text-center text-sm">No feed events yet.</div>}
            {feed.map((item) => (
              <article key={item.id} className="theme-panel rounded-3xl p-5">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <p className="theme-soft text-xs font-semibold uppercase tracking-[0.24em]">{item.eventType} / {item.visibility || "PUBLIC"}</p>
                    <h2 className="theme-title mt-2 text-xl font-bold">{item.title}</h2>
                    <p className="theme-muted mt-3 text-sm leading-7">{item.summary}</p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {item.ownPost && (
                      <button onClick={() => removePost(item.id)} className="rounded-xl border border-red-500/30 px-3 py-2 text-xs font-bold text-red-300">
                        Delete
                      </button>
                    )}
                    <button onClick={() => reportPost(item.id)} className="rounded-xl border border-amber-500/30 px-3 py-2 text-xs font-bold text-amber-300">
                      Report
                    </button>
                  </div>
                </div>

                {(item.mediaUrls || []).length > 0 && (
                  <div className="mt-4 grid gap-2 sm:grid-cols-2">
                    {(item.mediaUrls || []).map((url) => (
                      <div key={url} className="theme-card rounded-2xl px-3 py-2 text-xs theme-muted break-all">{url}</div>
                    ))}
                  </div>
                )}

                <div className="mt-4 flex flex-wrap items-center gap-2">
                  {["LIKE", "AMAZING", "GREAT_MATCH", "WELL_PLAYED"].map((reaction) => (
                    <button
                      key={reaction}
                      onClick={() => toggleReaction(item, reaction)}
                      className={`rounded-xl border px-3 py-2 text-xs font-bold ${
                        item.viewerReaction === reaction
                          ? "border-emerald-400/40 bg-emerald-500/15 text-emerald-200"
                          : "border-white/10 bg-white/5 theme-text"
                      }`}
                    >
                      {reaction.replace("_", " ")}
                    </button>
                  ))}
                </div>

                <div className="theme-soft mt-4 flex flex-wrap gap-3 text-xs uppercase tracking-wide">
                  <span>{item.reactionCount ?? 0} reactions</span>
                  <span>{item.commentCount ?? 0} comments</span>
                  <span>{item.viewCount ?? 0} views</span>
                  <span>Score {item.engagementScore ?? 0}</span>
                  <span>{item.createdAt?.replace("T", " ").slice(0, 16)}</span>
                </div>

                <div className="mt-4 space-y-3">
                  <button onClick={() => loadComments(item.id)} className="text-xs font-bold text-emerald-700 dark:text-emerald-200">
                    Load comments
                  </button>
                  {(comments[item.id] || []).map((comment) => (
                    <div key={comment.id} className="theme-card rounded-2xl p-3">
                      <p className="theme-title text-sm font-bold">{comment.authorName}</p>
                      <p className="theme-muted mt-1 text-sm">{comment.message}</p>
                    </div>
                  ))}
                  <div className="flex gap-2">
                    <input value={commentDrafts[item.id] || ""} onChange={(event) => setCommentDrafts({ ...commentDrafts, [item.id]: event.target.value })} placeholder="Write a comment" className={inputClass} />
                    <button onClick={() => submitComment(item.id)} className="rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-bold text-white">
                      Send
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </section>
      </div>
    </main>
  );
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { error?: string; message?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
