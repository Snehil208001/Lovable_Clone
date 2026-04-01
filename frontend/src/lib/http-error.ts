import { AxiosError } from "axios";

type ApiErrorShape = {
  message?: string;
  error?: string;
  status?: string;
};

export function getAxiosErrorMessage(error: unknown, fallback: string): string {
  const axiosError = error as AxiosError<ApiErrorShape | string>;
  const data = axiosError.response?.data;

  if (typeof data === "string") {
    try {
      const parsed = JSON.parse(data) as ApiErrorShape;
      if (parsed?.message) {
        return parsed.message;
      }
    } catch {
      if (data.trim().length > 0) {
        return data;
      }
    }
  }

  if (data && typeof data === "object") {
    if ("message" in data && typeof data.message === "string" && data.message.trim().length > 0) {
      return data.message;
    }
    if ("error" in data && typeof data.error === "string" && data.error.trim().length > 0) {
      return data.error;
    }
  }

  if (axiosError.message?.trim()) {
    return axiosError.message;
  }

  return fallback;
}
