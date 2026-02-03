import axios from "axios";

/**
 * Interface para erros retornados pela API
 */
export interface ApiError {
  status: number;
  type: string;
  message: string;
  details?: Record<string, string>;
}

/**
 * Extrai mensagem de erro apropriada de erros desconhecidos
 */
export function getErrorMessage(error: unknown): string {
  // Erro do Axios (HTTP)
  if (axios.isAxiosError(error)) {
    const apiError = error.response?.data as ApiError;

    // Se há detalhes de validação, mostrar todos
    if (apiError?.details) {
      return Object.values(apiError.details).join(", ");
    }

    // Mensagens específicas por status code
    switch (error.response?.status) {
      case 400:
        return "Dados inválidos. Verifique os campos e tente novamente.";
      case 401:
        return "Você não está autenticado. Faça login novamente.";
      case 403:
        return "Você não tem permissão para esta ação.";
      case 404:
        return "Stream não encontrada ou não existe.";
      case 409:
        return "Conflito: esta ação não pode ser realizada no estado atual.";
      case 422:
        return apiError?.message || "Dados inválidos.";
      case 429:
        return "Muitas requisições. Aguarde um momento e tente novamente.";
      case 500:
        return "Erro no servidor. Tente novamente em alguns instantes.";
      case 503:
        return "Serviço temporariamente indisponível. Tente novamente em breve.";
      default:
        return apiError?.message || "Erro desconhecido. Tente novamente.";
    }
  }

  // Erro de rede
  if (error instanceof Error) {
    if (error.message.includes("Network Error")) {
      return "Erro de conexão. Verifique sua internet e tente novamente.";
    }
    if (error.message.includes("timeout")) {
      return "Tempo de resposta excedido. Tente novamente.";
    }
    return error.message;
  }

  // Erro desconhecido
  return "Erro inesperado. Tente novamente.";
}

/**
 * Verifica se um erro é relacionado à rede
 */
export function isNetworkError(error: unknown): boolean {
  if (axios.isAxiosError(error)) {
    return !error.response && error.message === "Network Error";
  }
  return false;
}

/**
 * Verifica se um erro é um erro de timeout
 */
export function isTimeoutError(error: unknown): boolean {
  if (axios.isAxiosError(error)) {
    return error.code === "ECONNABORTED" || error.message.includes("timeout");
  }
  return false;
}

/**
 * Verifica se um erro deve ser retentado
 */
export function shouldRetry(error: unknown): boolean {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;

    // Retry em erros de rede, timeout, ou status codes específicos
    return (
      isNetworkError(error) ||
      isTimeoutError(error) ||
      status === 408 || // Request Timeout
      status === 429 || // Too Many Requests
      status === 503 || // Service Unavailable
      status === 504 // Gateway Timeout
    );
  }

  return false;
}

/**
 * Log de erro estruturado
 */
export function logError(error: unknown, context?: string): void {
  const timestamp = new Date().toISOString();
  const errorMessage = getErrorMessage(error);

  console.error("[Error]", {
    timestamp,
    context,
    message: errorMessage,
    error,
  });

  // Em produção, aqui você poderia enviar para um serviço de monitoramento
  // como Sentry, DataDog, etc.
}
