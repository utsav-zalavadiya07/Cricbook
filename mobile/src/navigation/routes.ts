export type AuthStackParamList = {
  Login: undefined;
  Register: undefined;
  Forgot: undefined;
};

export type PlayerTabParamList = {
  Home: undefined;
  Explore: undefined;
  Matches: undefined;
  Notifications: undefined;
  Profile: undefined;
};

export type OwnerTabParamList = {
  Dashboard: undefined;
  Turfs: undefined;
  Slots: undefined;
  Bookings: undefined;
  Revenue: undefined;
};

export type RootStackParamList = {
  Auth: undefined;
  PlayerPortal: undefined;
  OwnerPortal: undefined;
};

export const ROUTES = {
  AUTH: {
    LOGIN: "Login",
    REGISTER: "Register",
    FORGOT: "Forgot",
  } as const,
  PLAYER: {
    HOME: "Home",
    EXPLORE: "Explore",
    MATCHES: "Matches",
    NOTIFICATIONS: "Notifications",
    PROFILE: "Profile",
  } as const,
  OWNER: {
    DASHBOARD: "Dashboard",
    TURFS: "Turfs",
    SLOTS: "Slots",
    BOOKINGS: "Bookings",
    REVENUE: "Revenue",
  } as const,
};
