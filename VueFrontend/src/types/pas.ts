export interface VisualizationStep {
  title: string
  narration: string
  arrayState: number[]
  activeIndices: number[]
  highlightedLines: number[]
}

export interface UserProfile {
  id: number
  username: string
  creditsBalance: number
  frozenCreditsBalance: number
  status: string
}

export interface AuthResponse {
  token: string
  user: UserProfile
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
  sourceCode?: string | null
}

export interface GenerationTaskListItemResponse {
  id: number
  status: string
  detectedAlgorithm: string | null
  summary: string | null
  sourcePreview: string
  createdAt: string
  updatedAt: string
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

export interface ExportTaskListItemResponse {
  id: number
  generationTaskId: number
  status: string
  detectedAlgorithm: string | null
  createdAt: string
  updatedAt: string
}

export interface RechargePackageResponse {
  code: string
  name: string
  credits: number
  amountCents: number
  description: string
}

export interface RechargeOrderResponse {
  id: number
  packageCode: string
  packageName: string
  credits: number
  amountCents: number
  status: string
  createdAt: string
  paidAt: string | null
}
