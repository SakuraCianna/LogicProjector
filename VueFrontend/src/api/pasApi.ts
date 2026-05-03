import type {
  AuthResponse,
  CreateExportTaskResponse,
  ExportTaskListItemResponse,
  ExportTaskResponse,
  GenerationTaskListItemResponse,
  GenerationTaskResponse,
  RechargeOrderResponse,
  RechargePackageResponse,
  UserProfile,
} from "../types/pas";

const TOKEN_KEY = "pas_token";
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

function apiUrl(path: string): string {
  return `${API_BASE_URL}${path}`;
}

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export class AuthExpiredError extends ApiError {
  constructor(message = "登录已过期，请重新登录") {
    super(message, 401);
    this.name = "AuthExpiredError";
  }
}

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setStoredToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearStoredToken() {
  localStorage.removeItem(TOKEN_KEY);
}

function authHeaders(): HeadersInit {
  const token = getStoredToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function readErrorMessage(
  response: Response,
  fallback: string,
): Promise<string> {
  const payload = await response.json().catch(() => null);
  return typeof payload?.message === "string" && payload.message.length > 0
    ? payload.message
    : fallback;
}

async function requestJson<T>(
  url: string,
  init: RequestInit,
  fallback: string,
): Promise<T> {
  const response = await fetch(url, init);

  if (!response.ok) {
    const message = await readErrorMessage(response, fallback);
    if (response.status === 401) {
      throw new AuthExpiredError(message);
    }
    throw new ApiError(message, response.status);
  }

  return response.json() as Promise<T>;
}

export async function register(
  username: string,
  password: string,
): Promise<UserProfile> {
  return requestJson<UserProfile>(
    apiUrl("/api/auth/register"),
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ username, password }),
    },
    "注册失败",
  );
}

export async function login(
  username: string,
  password: string,
): Promise<AuthResponse> {
  return requestJson<AuthResponse>(
    apiUrl("/api/auth/login"),
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ username, password }),
    },
    "登录失败",
  );
}

export async function me(): Promise<UserProfile> {
  return requestJson<UserProfile>(
    apiUrl("/api/auth/me"),
    {
      headers: {
        ...authHeaders(),
      },
    },
    "登录状态校验失败",
  );
}

export async function createGenerationTask(
  sourceCode: string,
  language = "java",
): Promise<GenerationTaskResponse> {
  return requestJson<GenerationTaskResponse>(
    apiUrl("/api/generation-tasks"),
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...authHeaders(),
      },
      body: JSON.stringify({
        sourceCode,
        language,
      }),
    },
    "生成失败",
  );
}

export async function getGenerationTask(
  taskId: number,
): Promise<GenerationTaskResponse> {
  return requestJson<GenerationTaskResponse>(
    apiUrl(`/api/generation-tasks/${taskId}`),
    {
      headers: {
        ...authHeaders(),
      },
    },
    "生成状态刷新失败",
  );
}

export async function getRecentGenerationTasks(): Promise<
  GenerationTaskListItemResponse[]
> {
  return requestJson<GenerationTaskListItemResponse[]>(
    apiUrl("/api/generation-tasks/recent"),
    {
      headers: {
        ...authHeaders(),
      },
    },
    "最近生成记录加载失败",
  );
}

export async function createExportTask(
  taskId: number,
): Promise<CreateExportTaskResponse> {
  return requestJson<CreateExportTaskResponse>(
    apiUrl(`/api/generation-tasks/${taskId}/exports`),
    {
      method: "POST",
      headers: {
        ...authHeaders(),
      },
    },
    "导出任务创建失败",
  );
}

export async function getExportTask(
  exportTaskId: number,
): Promise<ExportTaskResponse> {
  return requestJson<ExportTaskResponse>(
    apiUrl(`/api/export-tasks/${exportTaskId}`),
    {
      headers: {
        ...authHeaders(),
      },
    },
    "导出状态刷新失败",
  );
}

export async function getRecentExportTasks(): Promise<
  ExportTaskListItemResponse[]
> {
  return requestJson<ExportTaskListItemResponse[]>(
    apiUrl("/api/export-tasks/recent"),
    {
      headers: {
        ...authHeaders(),
      },
    },
    "最近导出记录加载失败",
  );
}

export async function getRechargePackages(): Promise<
  RechargePackageResponse[]
> {
  return requestJson<RechargePackageResponse[]>(
    apiUrl("/api/recharge/packages"),
    {
      headers: {
        ...authHeaders(),
      },
    },
    "充值套餐加载失败",
  );
}

export async function getRecentRechargeOrders(): Promise<
  RechargeOrderResponse[]
> {
  return requestJson<RechargeOrderResponse[]>(
    apiUrl("/api/recharge/orders/recent"),
    {
      headers: {
        ...authHeaders(),
      },
    },
    "充值记录加载失败",
  );
}

export async function createRechargeOrder(
  packageCode: string,
): Promise<RechargeOrderResponse> {
  return requestJson<RechargeOrderResponse>(
    apiUrl("/api/recharge/orders"),
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...authHeaders(),
      },
      body: JSON.stringify({ packageCode }),
    },
    "充值订单创建失败",
  );
}

export async function simulateRechargePayment(
  orderId: number,
): Promise<RechargeOrderResponse> {
  return requestJson<RechargeOrderResponse>(
    apiUrl(`/api/recharge/orders/${orderId}/simulate-payment`),
    {
      method: "POST",
      headers: {
        ...authHeaders(),
      },
    },
    "模拟支付失败",
  );
}

export async function downloadExportVideo(
  exportTaskId: number,
  filename: string,
): Promise<void> {
  const url = apiUrl(`/api/export-tasks/${exportTaskId}/download`);
  const response = await fetch(url, {
    headers: {
      ...authHeaders(),
    },
  });

  if (!response.ok) {
    const message = await readErrorMessage(response, "下载失败");
    if (response.status === 401) {
      throw new AuthExpiredError(message);
    }
    throw new ApiError(message, response.status);
  }

  const blob = await response.blob();
  const downloadUrl = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = downloadUrl;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(downloadUrl);
}
