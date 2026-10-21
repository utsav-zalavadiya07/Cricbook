"use client";
import { TileInfo } from "@/components/ui";

import Link from "next/link";
import { type ReactNode } from "react";
import { FormEvent, useEffect, useState } from "react";
import {
  addCommerceCartItem,
  checkoutCommerceCart,
  CommerceCart,
  CommerceProduct,
  getCommerceCart,
  getCommerceProducts,
  removeCommerceCartItem,
} from "@/modules/commerce/services/commerceService";
import {
  bookTravelAccommodation,
  bookTravelPackage,
  buyTravelTicket,
  getTravelAccommodations,
  getTravelEvents,
  getTravelPackages,
  TravelAccommodation,
  TravelEvent,
  TravelPackage,
} from "@/modules/travel/services/travelService";
import {
  buyResaleListing,
  createResaleListing,
  getMyResaleListings,
  getMyResaleWaitlist,
  getRecommendedResaleListings,
  getResaleListings,
  joinResaleWaitlist,
  ResaleListing,
  SlotWaitlist,
} from "@/modules/resale/services/resaleService";
import { useToast } from "@/modules/ui/ToastProvider";
import { formatDate, formatDateTime } from "@/utils/dateFormatter";

export default function MarketplacePage() {
  const { toast } = useToast();
  const [city, setCity] = useState(() => (typeof window === "undefined" ? "Ahmedabad" : localStorage.getItem("globalCity") || "Ahmedabad"));
  const [listings, setListings] = useState<ResaleListing[]>([]);
  const [recommended, setRecommended] = useState<ResaleListing[]>([]);
  const [myListings, setMyListings] = useState<ResaleListing[]>([]);
  const [waitlist, setWaitlist] = useState<SlotWaitlist[]>([]);
  const [commerceProducts, setCommerceProducts] = useState<CommerceProduct[]>([]);
  const [commerceCart, setCommerceCart] = useState<CommerceCart | null>(null);
  const [commerceFilters, setCommerceFilters] = useState({ query: "", sport: "", category: "" });
  const [checkoutDraft, setCheckoutDraft] = useState({ shippingAddress: "Ahmedabad sports hub", couponCode: "", loyaltyPoints: "0" });
  const [travelPackages, setTravelPackages] = useState<TravelPackage[]>([]);
  const [travelEvents, setTravelEvents] = useState<TravelEvent[]>([]);
  const [travelAccommodations, setTravelAccommodations] = useState<TravelAccommodation[]>([]);
  const [travelFilters, setTravelFilters] = useState({ query: "", sport: "Cricket", destination: "", city: "Ahmedabad" });
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState({ bookingId: "", price: "", expiresAt: "", notes: "" });
  const [waitDraft, setWaitDraft] = useState({ slotId: "", date: new Date().toISOString().slice(0, 10), priority: "5" });

  async function load() {
    setLoading(true);
    try {
      const [listRes, recRes, myRes, waitRes] = await Promise.all([
        getResaleListings(city),
        getRecommendedResaleListings(),
        getMyResaleListings(),
        getMyResaleWaitlist(),
      ]);
      const productRes = await getCommerceProducts({
        query: commerceFilters.query || undefined,
        sport: commerceFilters.sport || undefined,
        category: commerceFilters.category || undefined,
      });
      const [packageRes, eventRes, accommodationRes] = await Promise.all([
        getTravelPackages({
          query: travelFilters.query || undefined,
          sport: travelFilters.sport || undefined,
          destination: travelFilters.destination || undefined,
        }),
        getTravelEvents({
          query: travelFilters.query || undefined,
          sport: travelFilters.sport || undefined,
          city: travelFilters.city || undefined,
        }),
        getTravelAccommodations({ city: travelFilters.city || undefined }),
      ]);
      setListings(listRes.data || []);
      setRecommended(recRes.data || []);
      setMyListings(myRes.data || []);
      setWaitlist(waitRes.data || []);
      setCommerceProducts(productRes.data || []);
      setTravelPackages(packageRes.data || []);
      setTravelEvents(eventRes.data || []);
      setTravelAccommodations(accommodationRes.data || []);
      try {
        const cartRes = await getCommerceCart();
        setCommerceCart(cartRes.data);
      } catch {
        setCommerceCart(null);
      }
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not load resale marketplace."), "error");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const createListing = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await createResaleListing({
        bookingId: Number(draft.bookingId),
        price: Number(draft.price),
        expiresAt: draft.expiresAt || undefined,
        notes: draft.notes || undefined,
      });
      toast("Slot listed in marketplace.", "success");
      setDraft({ bookingId: "", price: "", expiresAt: "", notes: "" });
      await load();
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not create resale listing."), "error");
    }
  };

  const joinWaitlist = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await joinResaleWaitlist({ slotId: Number(waitDraft.slotId), date: waitDraft.date, city, priority: Number(waitDraft.priority) });
      toast("Waitlist preference saved.", "success");
      setWaitDraft((current) => ({ ...current, slotId: "" }));
      await load();
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not join waitlist."), "error");
    }
  };

  const buy = async (listingId: number) => {
    try {
      await buyResaleListing(listingId, `WEB-${Date.now()}`);
      toast("Slot transferred to your bookings.", "success");
      await load();
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not buy this slot."), "error");
    }
  };

  const addProduct = async (product: CommerceProduct) => {
    const variant = product.variants?.find((item) => item.availableStock > 0) || product.variants?.[0];
    if (!variant) {
      toast("No inventory variant is available for this product.", "error");
      return;
    }
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);
    try {
      const response = await addCommerceCartItem({
        productId: product.id,
        inventoryId: variant.id,
        quantity: 1,
        rentalStart: product.productType === "RENTAL" ? today.toISOString().slice(0, 10) : undefined,
        rentalEnd: product.productType === "RENTAL" ? tomorrow.toISOString().slice(0, 10) : undefined,
      });
      setCommerceCart(response.data);
      toast("Product added to cart.", "success");
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not add product to cart."), "error");
    }
  };

  const removeCartItem = async (itemId: number) => {
    try {
      const response = await removeCommerceCartItem(itemId);
      setCommerceCart(response.data);
      toast("Cart updated.", "success");
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not remove cart item."), "error");
    }
  };

  const checkout = async () => {
    try {
      const response = await checkoutCommerceCart({
        shippingAddress: checkoutDraft.shippingAddress,
        couponCode: checkoutDraft.couponCode || undefined,
        loyaltyPoints: Number(checkoutDraft.loyaltyPoints || 0),
        paymentReference: `WEB-COM-${Date.now()}`,
      });
      setCommerceCart({ items: [], subtotal: 0, rentalDeposits: 0, itemCount: 0 });
      toast(`Order ${response.data.orderNumber} placed.`, "success");
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not checkout cart."), "error");
    }
  };

  const reservePackage = async (packageId: number) => {
    try {
      await bookTravelPackage(packageId, {
        bookingType: "INDIVIDUAL",
        participantCount: 1,
        couponCode: "TRAVEL10",
        paymentReference: `WEB-TRAVEL-${Date.now()}`,
      });
      toast("Travel package booked.", "success");
      await load();
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not book travel package."), "error");
    }
  };

  const reserveTicket = async (event: TravelEvent) => {
    const ticketType = event.ticketTypes?.find((item) => item.availableInventory > 0);
    if (!ticketType) {
      toast("No ticket inventory is available for this event.", "error");
      return;
    }
    try {
      await buyTravelTicket(event.id, { ticketTypeId: ticketType.id, quantity: 1 });
      toast("Travel event ticket issued.", "success");
      await load();
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not buy event ticket."), "error");
    }
  };

  const reserveRoom = async (accommodationId: number) => {
    const checkIn = new Date();
    checkIn.setDate(checkIn.getDate() + 7);
    const checkOut = new Date(checkIn);
    checkOut.setDate(checkIn.getDate() + 2);
    try {
      await bookTravelAccommodation(accommodationId, {
        checkIn: checkIn.toISOString().slice(0, 10),
        checkOut: checkOut.toISOString().slice(0, 10),
        rooms: 1,
      });
      toast("Accommodation booked.", "success");
      await load();
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not book accommodation."), "error");
    }
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-7xl space-y-8">
        <section className="theme-panel-strong overflow-hidden rounded-[2rem] p-6 sm:p-8">
          <p className="theme-accent text-xs font-black uppercase tracking-[0.34em]">EPIC 29 · Sports Commerce Marketplace</p>
          <div className="mt-4 grid gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-end">
            <div>
              <h1 className="theme-title text-4xl font-black tracking-tight sm:text-6xl">Buy gear, rent kits, and recover unused slots.
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Buy gear, rent kits, and recover unused slots. information" focusable={false}>Browse sports equipment, rental kits, seller inventory, and last-minute cricket slot transfers from one marketplace.</TileInfo>
              </span></h1>
            </div>
            <div className="theme-card-strong rounded-3xl p-4">
              <div className="grid gap-3 sm:grid-cols-[1fr_auto]">
                <Field label="Marketplace city" value={city} onChange={setCity} />
                <button onClick={load} className="mt-6 rounded-xl bg-emerald-500 px-5 py-2 text-sm font-black text-white">Refresh Slots</button>
              </div>
              <div className="mt-4 grid grid-cols-3 gap-3">
                <Metric label="Products" value={commerceProducts.length} />
                <Metric label="Cart" value={commerceCart?.itemCount ?? 0} />
                <Metric label="Slots" value={listings.length} />
              </div>
            </div>
          </div>
        </section>

        <section className="theme-panel rounded-3xl p-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="theme-accent text-xs font-black uppercase tracking-[0.24em]">EPIC 30 · Sports Travel</p>
              <h2 className="theme-title mt-2 text-xl font-black">Travel, Events, Camps & Experiences</h2>
              <p className="theme-muted mt-1 text-sm">Package discovery, ticketing, accommodation inventory, and experience booking in one marketplace layer.</p>
            </div>
            <div className="grid gap-3 sm:grid-cols-5 lg:min-w-[760px]">
              <Field label="Search" value={travelFilters.query} onChange={(value) => setTravelFilters((current) => ({ ...current, query: value }))} />
              <Field label="Sport" value={travelFilters.sport} onChange={(value) => setTravelFilters((current) => ({ ...current, sport: value }))} />
              <Field label="Destination" value={travelFilters.destination} onChange={(value) => setTravelFilters((current) => ({ ...current, destination: value }))} />
              <Field label="City" value={travelFilters.city} onChange={(value) => setTravelFilters((current) => ({ ...current, city: value }))} />
              <button onClick={load} className="mt-6 rounded-xl border border-emerald-500/30 px-4 py-2 text-sm font-black text-emerald-300">Find travel</button>
            </div>
          </div>
          <div className="mt-5 grid gap-4 xl:grid-cols-3">
            <TravelColumn title="Travel Packages" empty="No travel packages published yet.">
              {travelPackages.map((item) => (
                <TravelPackageCard key={item.id} item={item} onReserve={reservePackage} />
              ))}
            </TravelColumn>
            <TravelColumn title="Events & Camps" empty="No events or camps published yet.">
              {travelEvents.map((item) => (
                <TravelEventCard key={item.id} item={item} onReserve={reserveTicket} />
              ))}
            </TravelColumn>
            <TravelColumn title="Accommodation" empty="No accommodation inventory published yet.">
              {travelAccommodations.map((item) => (
                <AccommodationCard key={item.id} item={item} onReserve={reserveRoom} />
              ))}
            </TravelColumn>
          </div>
        </section>

        <section className="grid gap-6 xl:grid-cols-[1.35fr_0.65fr]">
          <div className="theme-panel rounded-3xl p-6">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <h2 className="theme-title text-xl font-black">Sports Commerce</h2>
                <p className="theme-muted mt-1 text-sm">Equipment, team merchandise, rentals, and affiliate inventory with stock-aware checkout.</p>
              </div>
              <div className="grid gap-3 sm:grid-cols-4 lg:min-w-[620px]">
                <Field label="Search" value={commerceFilters.query} onChange={(value) => setCommerceFilters((current) => ({ ...current, query: value }))} />
                <Field label="Sport" value={commerceFilters.sport} onChange={(value) => setCommerceFilters((current) => ({ ...current, sport: value }))} />
                <Field label="Category" value={commerceFilters.category} onChange={(value) => setCommerceFilters((current) => ({ ...current, category: value }))} />
                <button onClick={load} className="mt-6 rounded-xl border border-emerald-500/30 px-4 py-2 text-sm font-black text-emerald-300">Apply</button>
              </div>
            </div>
            <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {loading && <p className="theme-muted col-span-full py-8 text-center">Loading products...</p>}
              {!loading && commerceProducts.length === 0 && <p className="theme-card theme-muted col-span-full rounded-2xl border-dashed p-8 text-center">No commerce products found yet.</p>}
              {commerceProducts.map((product) => (
                <ProductCard key={product.id} product={product} onAdd={addProduct} />
              ))}
            </div>
          </div>

          <aside className="theme-panel rounded-3xl p-6">
            <h2 className="theme-title text-xl font-black">Commerce Cart</h2>
            <p className="theme-muted mt-1 text-sm">Checkout supports coupons, loyalty points, rentals, shipping, and inventory reservation.</p>
            <div className="mt-5 space-y-3">
              {(commerceCart?.items || []).length === 0 && <p className="theme-card theme-muted rounded-2xl border-dashed p-6 text-center text-sm">Your commerce cart is empty.</p>}
              {(commerceCart?.items || []).map((item) => (
                <div key={item.id} className="theme-panel-soft rounded-2xl p-3">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="theme-title text-sm font-black">{item.product.name}</p>
                      <p className="theme-muted mt-1 text-xs">{item.inventory.variantLabel} · Qty {item.quantity}</p>
                    </div>
                    <button onClick={() => removeCartItem(item.id)} className="text-xs font-black text-rose-300">Remove</button>
                  </div>
                  <p className="theme-accent mt-2 text-sm font-black">Rs {item.lineTotal}</p>
                </div>
              ))}
            </div>
            <div className="mt-5 grid gap-3">
              <Field label="Shipping address" value={checkoutDraft.shippingAddress} onChange={(value) => setCheckoutDraft((current) => ({ ...current, shippingAddress: value }))} />
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="Coupon" value={checkoutDraft.couponCode} onChange={(value) => setCheckoutDraft((current) => ({ ...current, couponCode: value }))} />
                <Field label="Loyalty points" value={checkoutDraft.loyaltyPoints} onChange={(value) => setCheckoutDraft((current) => ({ ...current, loyaltyPoints: value }))} type="number" />
              </div>
              <div className="theme-card rounded-2xl p-4">
                <div className="flex items-center justify-between text-sm">
                  <span className="theme-muted">Subtotal</span>
                  <span className="theme-title font-black">Rs {commerceCart?.subtotal ?? 0}</span>
                </div>
                <div className="mt-2 flex items-center justify-between text-sm">
                  <span className="theme-muted">Rental deposits</span>
                  <span className="theme-title font-black">Rs {commerceCart?.rentalDeposits ?? 0}</span>
                </div>
              </div>
              <button onClick={checkout} disabled={!commerceCart?.itemCount} className="rounded-2xl bg-emerald-500 px-5 py-3 text-sm font-black text-white shadow-lg shadow-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-50">Checkout Commerce Cart</button>
            </div>
          </aside>
        </section>

        <section className="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]">
          <div className="space-y-6">
            <form onSubmit={createListing} className="theme-panel rounded-3xl p-6">
              <h2 className="theme-title text-xl font-black">List Your Booking</h2>
              <p className="theme-muted mt-1 text-sm">Confirmed bookings can be resold within owner transfer rules.</p>
              <div className="mt-5 grid gap-4">
                <Field label="Booking ID" value={draft.bookingId} onChange={(value) => setDraft((current) => ({ ...current, bookingId: value }))} type="number" />
                <Field label="Resale Price" value={draft.price} onChange={(value) => setDraft((current) => ({ ...current, price: value }))} type="number" />
                <Field label="Expires At" value={draft.expiresAt} onChange={(value) => setDraft((current) => ({ ...current, expiresAt: value }))} type="datetime-local" />
                <Field label="Notes" value={draft.notes} onChange={(value) => setDraft((current) => ({ ...current, notes: value }))} />
              </div>
              <button className="mt-5 w-full rounded-2xl bg-emerald-500 px-5 py-3 text-sm font-black text-white shadow-lg shadow-emerald-500/20">Publish Listing</button>
            </form>

            <form onSubmit={joinWaitlist} className="theme-panel rounded-3xl p-6">
              <h2 className="theme-title text-xl font-black">Priority Waitlist</h2>
              <p className="theme-muted mt-1 text-sm">Get notified when a preferred booked slot enters resale.</p>
              <div className="mt-5 grid gap-4 sm:grid-cols-3">
                <Field label="Slot ID" value={waitDraft.slotId} onChange={(value) => setWaitDraft((current) => ({ ...current, slotId: value }))} type="number" />
                <Field label="Date" value={waitDraft.date} onChange={(value) => setWaitDraft((current) => ({ ...current, date: value }))} type="date" />
                <Field label="Priority" value={waitDraft.priority} onChange={(value) => setWaitDraft((current) => ({ ...current, priority: value }))} type="number" />
              </div>
              <button className="mt-5 rounded-2xl border border-emerald-500/40 px-5 py-3 text-sm font-black text-emerald-300">Notify Me</button>
            </form>
          </div>

          <div className="space-y-6">
            <ListingGrid title="Recommended Slots" loading={loading} listings={recommended} onBuy={buy} />
            <ListingGrid title="Live Marketplace" loading={loading} listings={listings} onBuy={buy} />
            <ListingGrid title="My Listings" loading={loading} listings={myListings} onBuy={undefined} />
          </div>
        </section>
      </div>
    </main>
  );
}

function TravelColumn({ title, empty, children }: { title: string; empty: string; children: ReactNode }) {
  const hasChildren = Array.isArray(children) ? children.length > 0 : Boolean(children);
  return (
    <div className="theme-panel-soft rounded-2xl p-4">
      <h3 className="theme-title font-black">{title}</h3>
      <div className="mt-4 space-y-3">
        {hasChildren ? children : <p className="theme-card theme-muted rounded-2xl border-dashed p-6 text-center text-sm">{empty}</p>}
      </div>
    </div>
  );
}

function TravelPackageCard({ item, onReserve }: { item: TravelPackage; onReserve: (packageId: number) => void }) {
  return (
    <article className="theme-card rounded-2xl p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="theme-title text-sm font-black">{item.title}</p>
          <p className="theme-muted mt-1 text-xs">{item.originCity || "Any city"} → {item.destination}</p>
        </div>
        <span className="theme-badge rounded-full px-3 py-1 text-[10px] font-black">{item.packageType}</span>
      </div>
      <p className="theme-muted mt-3 text-xs">{formatDate(item.startDate)} to {formatDate(item.endDate)} · {item.availableCapacity} seats left</p>
      <div className="mt-3 flex flex-wrap gap-2 text-[11px] font-bold">
        {item.includesAccommodation && <span className="theme-panel-soft rounded-full px-3 py-1">Hotel</span>}
        {item.includesTransport && <span className="theme-panel-soft rounded-full px-3 py-1">Transport</span>}
        <span className="theme-panel-soft rounded-full px-3 py-1">{item.sport}</span>
      </div>
      <div className="mt-4 flex items-center justify-between gap-3">
        <p className="theme-accent font-black">Rs {item.pricePerPerson}</p>
        <button onClick={() => onReserve(item.id)} disabled={item.availableCapacity <= 0} className="rounded-xl bg-emerald-500 px-4 py-2 text-xs font-black text-white disabled:cursor-not-allowed disabled:opacity-50">Book</button>
      </div>
    </article>
  );
}

function TravelEventCard({ item, onReserve }: { item: TravelEvent; onReserve: (event: TravelEvent) => void }) {
  const lowestTicket = item.ticketTypes?.filter((ticket) => ticket.active && ticket.availableInventory > 0).sort((left, right) => left.price - right.price)[0];
  return (
    <article className="theme-card rounded-2xl p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="theme-title text-sm font-black">{item.title}</p>
          <p className="theme-muted mt-1 text-xs">{item.city} · {item.venue || "Venue TBA"}</p>
        </div>
        <span className="theme-badge rounded-full px-3 py-1 text-[10px] font-black">{item.eventType}</span>
      </div>
      <p className="theme-muted mt-3 text-xs">{formatDateTime(item.startsAt)} · {item.availableCapacity} spots left</p>
      <div className="mt-4 flex items-center justify-between gap-3">
        <p className="theme-accent font-black">Rs {lowestTicket?.price ?? item.basePrice}</p>
        <button onClick={() => onReserve(item)} disabled={!lowestTicket} className="rounded-xl bg-emerald-500 px-4 py-2 text-xs font-black text-white disabled:cursor-not-allowed disabled:opacity-50">Ticket</button>
      </div>
    </article>
  );
}

function AccommodationCard({ item, onReserve }: { item: TravelAccommodation; onReserve: (accommodationId: number) => void }) {
  return (
    <article className="theme-card rounded-2xl p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="theme-title text-sm font-black">{item.name}</p>
          <p className="theme-muted mt-1 text-xs">{item.city} · {item.roomType}</p>
        </div>
        <span className="theme-badge rounded-full px-3 py-1 text-[10px] font-black">{item.accommodationType}</span>
      </div>
      <p className="theme-muted mt-3 text-xs">{item.partnerName || "Partner managed"} · {item.availableRooms} rooms left</p>
      <div className="mt-4 flex items-center justify-between gap-3">
        <p className="theme-accent font-black">Rs {item.pricePerNight}/night</p>
        <button onClick={() => onReserve(item.id)} disabled={item.availableRooms <= 0} className="rounded-xl bg-emerald-500 px-4 py-2 text-xs font-black text-white disabled:cursor-not-allowed disabled:opacity-50">Room</button>
      </div>
    </article>
  );
}

function ProductCard({ product, onAdd }: { product: CommerceProduct; onAdd: (product: CommerceProduct) => void }) {
  const priceLabel = product.productType === "RENTAL" ? `Rs ${product.rentalFeePerDay ?? product.price}/day` : `Rs ${product.price}`;
  return (
    <article className="theme-panel-soft rounded-2xl p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="theme-title font-black">{product.name}</p>
          <p className="theme-muted mt-1 text-sm">{product.brand || product.sellerName} · {product.sport}</p>
        </div>
        <span className="theme-badge rounded-full px-3 py-1 text-xs font-black">{product.productType}</span>
      </div>
      <p className="theme-muted mt-3 line-clamp-2 text-sm">{product.description || "Sports commerce product ready for marketplace checkout."}</p>
      <div className="mt-4 flex flex-wrap gap-2 text-xs font-bold">
        <span className="theme-card rounded-full px-3 py-1">{product.category}</span>
        <span className="theme-card rounded-full px-3 py-1">Stock {product.availableStock}</span>
        <span className="theme-card rounded-full px-3 py-1">Rating {product.rating ?? 0}</span>
      </div>
      <div className="mt-4 flex items-center justify-between gap-3">
        <p className="theme-accent text-lg font-black">{priceLabel}</p>
        {product.productType === "AFFILIATE" && product.affiliateUrl ? (
          <a href={product.affiliateUrl} target="_blank" rel="noreferrer" className="rounded-xl border border-emerald-500/30 px-4 py-2 text-sm font-black text-emerald-300">Open</a>
        ) : (
          <button onClick={() => onAdd(product)} disabled={product.availableStock <= 0} className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-black text-white disabled:cursor-not-allowed disabled:opacity-50">Add</button>
        )}
      </div>
    </article>
  );
}

function ListingGrid({ title, loading, listings, onBuy }: { title: string; loading: boolean; listings: ResaleListing[]; onBuy?: (id: number) => void }) {
  return (
    <section className="theme-panel rounded-3xl p-6">
      <h2 className="theme-title text-xl font-black">{title}</h2>
      <div className="mt-5 grid gap-4 lg:grid-cols-2">
        {loading && <p className="theme-muted col-span-full py-8 text-center">Loading slots...</p>}
        {!loading && listings.length === 0 && <p className="theme-card theme-muted col-span-full rounded-2xl border-dashed p-8 text-center">No resale slots found.</p>}
        {listings.map((listing) => (
          <article key={`${title}-${listing.id}`} className="theme-panel-soft rounded-2xl p-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="theme-title font-black">{listing.turfName}</p>
                <p className="theme-muted mt-1 text-sm">{listing.city} · {formatDate(listing.bookingDate)} · {listing.startTime}</p>
              </div>
              <span className="theme-badge rounded-full px-3 py-1 text-xs font-black">₹{listing.price}</span>
            </div>
            <p className="theme-muted mt-3 text-sm">{listing.recommendationReason}</p>
            <div className="mt-4 flex flex-wrap gap-2 text-xs font-bold">
              <span className="theme-card rounded-full px-3 py-1">Trust {listing.sellerTrustScore}</span>
              <span className="theme-card rounded-full px-3 py-1">{listing.recommendationScore}% fit</span>
              <span className="theme-card rounded-full px-3 py-1">{listing.status}</span>
            </div>
            <div className="mt-4 grid grid-cols-2 gap-2">
              <Link href={`/marketplace/${listing.id}`} className="rounded-xl border border-emerald-500/30 px-4 py-2 text-center text-sm font-black text-emerald-300">Details</Link>
              {onBuy && listing.status === "ACTIVE" && (
                <button onClick={() => onBuy(listing.id)} className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-black text-white">Buy Slot</button>
              )}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-panel-soft rounded-2xl p-3">
      <p className="theme-soft text-[10px] font-black uppercase tracking-[0.2em]">{label}</p>
      <p className="theme-title mt-2 text-2xl font-black">{value}</p>
    </div>
  );
}

function Field({ label, value, onChange, type = "text" }: { label: string; value: string; onChange: (value: string) => void; type?: string }) {
  return (
    <label>
      <span className="theme-title text-xs font-black uppercase tracking-wider">{label}</span>
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} className="theme-input mt-2 w-full rounded-xl px-3 py-2 text-sm outline-none" />
    </label>
  );
}

function apiMessage(error: unknown, fallback: string) {
  const response = (error as { response?: { data?: { message?: string } } } | null)?.response;
  return response?.data?.message || fallback;
}
