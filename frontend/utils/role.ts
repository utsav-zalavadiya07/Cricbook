export const isOwner = () => {
  if (typeof window !== "undefined") {
    return localStorage.getItem("role") === "OWNER";
  }
  return false;
};

export const isUser = () => {
  if (typeof window !== "undefined") {
    return localStorage.getItem("role") === "USER";
  }
  return false;
};
