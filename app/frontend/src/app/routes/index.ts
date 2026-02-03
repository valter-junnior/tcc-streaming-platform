export const routes = {
  home: "/",
  dashboard: (streamId: string) => `/dashboard/${streamId}`,
  watch: (streamId: string) => `/watch/${streamId}`,
} as const;
