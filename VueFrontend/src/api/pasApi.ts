import type { CreateExportTaskResponse, ExportTaskResponse, GenerationTaskResponse } from '../types/pas'

export async function createGenerationTask(sourceCode: string): Promise<GenerationTaskResponse> {
  const response = await fetch('http://localhost:8080/api/generation-tasks', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      userId: 1,
      sourceCode,
      language: 'java',
    }),
  })

  if (!response.ok) {
    const payload = await response.json().catch(() => ({ message: 'Generation failed' }))
    throw new Error(payload.message ?? 'Generation failed')
  }

  return response.json() as Promise<GenerationTaskResponse>
}

export async function createExportTask(taskId: number): Promise<CreateExportTaskResponse> {
  const response = await fetch(`http://localhost:8080/api/generation-tasks/${taskId}/exports`, {
    method: 'POST',
  })

  if (!response.ok) {
    const payload = await response.json().catch(() => ({ message: 'Export creation failed' }))
    throw new Error(payload.message ?? 'Export creation failed')
  }

  return response.json() as Promise<CreateExportTaskResponse>
}

export async function getExportTask(exportTaskId: number): Promise<ExportTaskResponse> {
  const response = await fetch(`http://localhost:8080/api/export-tasks/${exportTaskId}`)

  if (!response.ok) {
    const payload = await response.json().catch(() => ({ message: 'Export polling failed' }))
    throw new Error(payload.message ?? 'Export polling failed')
  }

  return response.json() as Promise<ExportTaskResponse>
}
