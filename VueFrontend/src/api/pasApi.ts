import type {
  AuthResponse,
  CreateExportTaskResponse,
  ExportTaskListItemResponse,
  ExportTaskResponse,
  GenerationTaskListItemResponse,
  GenerationTaskResponse,
  UserProfile,
} from '../types/pas'

const TOKEN_KEY = 'pas_token'
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

function apiUrl(path: string): string {
  return `${API_BASE_URL}${path}`
}

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export class AuthExpiredError extends ApiError {
  constructor(message = 'Login expired. Please sign in again.') {
    super(message, 403)
    this.name = 'AuthExpiredError'
  }
}

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setStoredToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearStoredToken() {
  localStorage.removeItem(TOKEN_KEY)
}

function authHeaders(): HeadersInit {
  const token = getStoredToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function readErrorMessage(response: Response, fallback: string): Promise<string> {
  const payload = await response.json().catch(() => null)
  return typeof payload?.message === 'string' && payload.message.length > 0
    ? payload.message
    : fallback
}

async function requestJson<T>(url: string, init: RequestInit, fallback: string): Promise<T> {
  const response = await fetch(url, init)

  if (!response.ok) {
    const message = await readErrorMessage(response, fallback)
    if (response.status === 401 || response.status === 403) {
      throw new AuthExpiredError(message)
    }
    throw new ApiError(message, response.status)
  }

  return response.json() as Promise<T>
}

export async function register(username: string, password: string): Promise<UserProfile> {
  return requestJson<UserProfile>(apiUrl('/api/auth/register'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ username, password }),
  }, 'Registration failed')
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  return requestJson<AuthResponse>(apiUrl('/api/auth/login'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ username, password }),
  }, 'Login failed')
}

export async function me(): Promise<UserProfile> {
  return requestJson<UserProfile>(apiUrl('/api/auth/me'), {
    headers: {
      ...authHeaders(),
    },
  }, 'Auth check failed')
}

export async function createGenerationTask(sourceCode: string): Promise<GenerationTaskResponse> {
  return requestJson<GenerationTaskResponse>(apiUrl('/api/generation-tasks'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(),
    },
    body: JSON.stringify({
      sourceCode,
      language: 'java',
    }),
  }, 'Generation failed')
}

export async function getGenerationTask(taskId: number): Promise<GenerationTaskResponse> {
  return requestJson<GenerationTaskResponse>(apiUrl(`/api/generation-tasks/${taskId}`), {
    headers: {
      ...authHeaders(),
    },
  }, 'Generation polling failed')
}

export async function getRecentGenerationTasks(): Promise<GenerationTaskListItemResponse[]> {
  return requestJson<GenerationTaskListItemResponse[]>(apiUrl('/api/generation-tasks/recent'), {
    headers: {
      ...authHeaders(),
    },
  }, 'Recent generation loading failed')
}

export async function createExportTask(taskId: number): Promise<CreateExportTaskResponse> {
  return requestJson<CreateExportTaskResponse>(apiUrl(`/api/generation-tasks/${taskId}/exports`), {
    method: 'POST',
    headers: {
      ...authHeaders(),
    },
  }, 'Export creation failed')
}

export async function getExportTask(exportTaskId: number): Promise<ExportTaskResponse> {
  return requestJson<ExportTaskResponse>(apiUrl(`/api/export-tasks/${exportTaskId}`), {
    headers: {
      ...authHeaders(),
    },
  }, 'Export polling failed')
}

export async function getRecentExportTasks(): Promise<ExportTaskListItemResponse[]> {
  return requestJson<ExportTaskListItemResponse[]>(apiUrl('/api/export-tasks/recent'), {
    headers: {
      ...authHeaders(),
    },
  }, 'Recent export loading failed')
}
