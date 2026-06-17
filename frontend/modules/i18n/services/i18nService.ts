import api from "@/services/api";

export const getGlobalTranslations = async (lang: string) => {
  const response = await api.get("/global/translations", {
    params: { lang },
  });
  return response.data as Record<string, string>;
};

export const getLanguagePreference = async () => {
  const response = await api.get("/users/language");
  return response.data as { preferredLanguage: string };
};

export const updateLanguagePreference = async (preferredLanguage: string) => {
  const response = await api.put(
    "/users/language",
    { preferredLanguage },
    {
      notification: {
        category: "system",
        success: {
          title: "Language Updated",
          message: "Your language preference has been saved.",
        },
      },
    }
  );
  return response.data as { preferredLanguage: string };
};
