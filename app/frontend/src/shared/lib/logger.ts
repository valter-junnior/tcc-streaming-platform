type LogLevel = "info" | "warn" | "error" | "debug";

class Logger {
  private isDev = import.meta.env.DEV;

  private log(level: LogLevel, message: string, data?: any) {
    if (this.isDev) {
      switch (level) {
        case "error":
          console.error(`[${level.toUpperCase()}]`, message, data || "");
          break;
        case "warn":
          console.warn(`[${level.toUpperCase()}]`, message, data || "");
          break;
        case "info":
          console.info(`[${level.toUpperCase()}]`, message, data || "");
          break;
        case "debug":
          console.debug(`[${level.toUpperCase()}]`, message, data || "");
          break;
      }
    } else if (level === "error") {
      console.error(message);
    }
  }

  info(message: string, data?: any) {
    this.log("info", message, data);
  }

  warn(message: string, data?: any) {
    this.log("warn", message, data);
  }

  error(message: string, data?: any) {
    this.log("error", message, data);
  }

  debug(message: string, data?: any) {
    this.log("debug", message, data);
  }
}

export const logger = new Logger();
