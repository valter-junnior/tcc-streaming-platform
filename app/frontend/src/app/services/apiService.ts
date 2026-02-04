import axios, { type AxiosInstance } from "axios";
import { API_URL } from "../config/env";
import type {
  Stream,
  CreateStreamRequest,
  CreateStreamResponse,
  StreamStatusResponse,
  UpdateStreamRequest,
} from "../types/stream";

class ApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: API_URL,
      timeout: 10000,
      headers: {
        "Content-Type": "application/json",
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors(): void {
    this.api.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem("token");
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error),
    );

    this.api.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          localStorage.removeItem("token");
        }

        if (error.response?.status >= 500) {
          console.error(
            "[API] Server error:",
            error.response.status,
            error.response.data,
          );
        }

        if (!error.response && error.message === "Network Error") {
          console.error("[API] Network error - check connection");
        }

        return Promise.reject(error);
      },
    );
  }

  async createStream(data: CreateStreamRequest): Promise<CreateStreamResponse> {
    const response = await this.api.post<CreateStreamResponse>(
      "/streams",
      data,
    );
    return response.data;
  }

  async getStream(id: string): Promise<Stream> {
    const response = await this.api.get<Stream>(`/streams/${id}`);
    return response.data;
  }

  async getStreamStatus(id: string): Promise<StreamStatusResponse> {
    const response = await this.api.get<StreamStatusResponse>(
      `/streams/${id}/status`,
    );
    return response.data;
  }

  async endStream(id: string, ownerId: string): Promise<void> {
    await this.api.delete(`/streams/${id}?ownerId=${ownerId}`);
  }

  async restartStream(id: string): Promise<Stream> {
    const response = await this.api.post<Stream>(`/streams/${id}/restart`);
    return response.data;
  }

  async getLiveStreams(): Promise<Stream[]> {
    const response = await this.api.get<Stream[]>("/streams/live");
    return response.data;
  }

  async getMyStreams(ownerId: string): Promise<Stream[]> {
    const response = await this.api.get<Stream[]>(
      `/streams/my?ownerId=${ownerId}`,
    );
    return response.data;
  }

  async updateStream(id: string, data: UpdateStreamRequest): Promise<Stream> {
    const response = await this.api.put<Stream>(`/streams/${id}`, data);
    return response.data;
  }

  async deleteStream(id: string, ownerId: string): Promise<void> {
    await this.api.delete(`/streams/${id}?ownerId=${ownerId}`);
  }
}

export const apiService = new ApiService();
