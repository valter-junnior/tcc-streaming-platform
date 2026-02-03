export interface AppConfig {
  timezone: string;
  version: string;
  environment: string;
  serverTimestamp: number;
}

class ConfigService {
  private config: AppConfig | null = null;
  private configPromise: Promise<AppConfig> | null = null;

  /**
   * Busca configurações do backend (com cache)
   */
  async getConfig(): Promise<AppConfig> {
    if (this.config) {
      return this.config;
    }

    if (this.configPromise) {
      return this.configPromise;
    }

    this.configPromise = this.fetchConfig();
    this.config = await this.configPromise;
    return this.config;
  }

  private async fetchConfig(): Promise<AppConfig> {
    try {
      const response = await fetch(
        `${import.meta.env.VITE_API_URL || "http://localhost:8080/api"}/config`,
      );

      if (!response.ok) {
        throw new Error(`Failed to fetch config: ${response.status}`);
      }

      const config: AppConfig = await response.json();
      return config;
    } catch (error) {
      console.error("[ConfigService] Error fetching config:", error);

      // Fallback para configuração padrão
      return {
        timezone: "UTC",
        version: "1.0.0",
        environment: "dev",
        serverTimestamp: Date.now(),
      };
    }
  }

  /**
   * Força recarregamento das configurações
   */
  async reloadConfig(): Promise<AppConfig> {
    this.config = null;
    this.configPromise = null;
    return this.getConfig();
  }
}

export const configService = new ConfigService();
