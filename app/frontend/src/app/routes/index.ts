export const routes = {
  home: () => "/",
  myStreams: () => "/my-streams",
  dashboard: (streamId: string) => `/dashboard/${streamId}`,
  watch: (streamId: string) => `/watch/${streamId}`,
} as const;
