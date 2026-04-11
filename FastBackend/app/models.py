from pydantic import BaseModel


class ExportRequest(BaseModel):
    task_id: int
    summary: str
    steps: list[dict]
    subtitle_enabled: bool = True
    tts_enabled: bool = False


class ExportResponse(BaseModel):
    task_id: int
    subtitle_path: str
    audio_path: str | None
    ffmpeg_command: list[str]
