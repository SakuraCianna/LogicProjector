from pydantic import BaseModel


class ExportRequest(BaseModel):
    exportTaskId: int
    generationTaskId: int
    algorithm: str
    summary: str
    visualizationPayload: dict
    sourceCode: str
    subtitleEnabled: bool = True
    ttsEnabled: bool = True


class ExportResponse(BaseModel):
    status: str
    progress: int
    videoPath: str
    subtitlePath: str | None
    audioPath: str | None
    tokenUsage: int
    renderSeconds: int
    concurrencyUnits: int
    errorMessage: str | None
