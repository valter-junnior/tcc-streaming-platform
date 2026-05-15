export const StreamStatus = {
  WAITING: "WAITING",
  LIVE: "LIVE",
  ENDED: "ENDED",
} as const;

export type StreamStatus = (typeof StreamStatus)[keyof typeof StreamStatus];

export interface Stream {
  id: string;
  title: string;
  description: string;
  streamKey: string;
  status: StreamStatus;
  ownerId: string;
  createdAt: string;
  startedAt: string | null;
  endedAt: string | null;
  currentViewers: number;
  viewersPeak: number;
}

export interface CreateStreamRequest {
  title: string;
  description: string;
  ownerId: string;
}

export interface CreateStreamResponse {
  id: string;
  streamKey: string;
  rtmpUrl: string;
  watchUrl: string;
}

export interface StreamStatusResponse {
  id: string;
  status: StreamStatus;
  currentViewers: number;
  viewersPeak: number;
}

export interface UpdateStreamRequest {
  title: string;
  description: string;
  ownerId: string;
}
