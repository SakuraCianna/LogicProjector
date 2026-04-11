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
