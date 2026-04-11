import type { GenerationTaskResponse } from '../types/pas'

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
