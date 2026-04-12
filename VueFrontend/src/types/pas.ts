export interface VisualizationStep {
  title: string
  narration: string
  arrayState: number[]
  activeIndices: number[]
  highlightedLines: number[]
}

export interface VisualizationPayload {
  algorithm: string
  steps: VisualizationStep[]
}

export interface GenerationTaskResponse {
  id: number
  status: string
  language: string
  detectedAlgorithm: string | null
  summary: string | null
  confidenceScore: number
  visualizationPayload: VisualizationPayload | null
  errorMessage: string | null
  creditsCharged: number
}

export interface CreateExportTaskResponse {
  id: number
  generationTaskId: number
  status: string
  progress: number
  creditsFrozen: number
}

export interface ExportTaskResponse {
  id: number
  generationTaskId: number
  status: string
  progress: number
  videoUrl: string | null
  subtitleUrl: string | null
  audioUrl: string | null
  errorMessage: string | null
  creditsFrozen: number | null
  creditsCharged: number | null
  createdAt: string
  updatedAt: string
}
