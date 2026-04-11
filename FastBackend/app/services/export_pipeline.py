from app.services.subtitle_service import SubtitleService
from app.services.tts_service import TtsService
from app.services.video_compositor import VideoCompositor


class ExportPipeline:
    def __init__(self) -> None:
        self.subtitle_service = SubtitleService()
        self.tts_service = TtsService()
        self.video_compositor = VideoCompositor()

    def build_job(self, payload: dict) -> dict:
        task_id = payload["task_id"]
        subtitle_path = self.subtitle_service.build_subtitle_path(task_id)
        audio_path = (
            self.tts_service.build_audio_path(task_id)
            if payload.get("tts_enabled")
            else None
        )
        ffmpeg_command = self.video_compositor.build_command(
            task_id, subtitle_path, audio_path
        )
        return {
            "task_id": task_id,
            "subtitle_path": subtitle_path,
            "audio_path": audio_path,
            "ffmpeg_command": ffmpeg_command,
        }
