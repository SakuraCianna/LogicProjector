from pathlib import Path

from app.services.frame_renderer import FrameRenderer
from app.services.subtitle_service import SubtitleService
from app.services.tts_service import TtsService
from app.services.video_compositor import VideoCompositor


class ExportPipeline:
    def __init__(self, output_root: Path | None = None) -> None:
        self.output_root = output_root or Path("outputs")
        self.frame_renderer = FrameRenderer(self.output_root)
        self.subtitle_service = SubtitleService(self.output_root)
        self.tts_service = TtsService(self.output_root)
        self.video_compositor = VideoCompositor()

    def build_export(self, payload: dict) -> dict:
        export_task_id = payload["exportTaskId"]
        steps = payload["visualizationPayload"]["steps"]
        narration_text = (
            payload["summary"] + " " + " ".join(step["narration"] for step in steps)
        )
        try:
            frame_dir = self.frame_renderer.render_frames(payload)
            subtitle_path = (
                self.subtitle_service.build_subtitle_file(export_task_id, steps)
                if payload.get("subtitleEnabled")
                else None
            )
            audio_path = (
                self.tts_service.build_audio_file(
                    export_task_id, payload["summary"], steps
                )
                if payload.get("ttsEnabled")
                else None
            )
            video_path, _command = self.video_compositor.compose(
                export_task_id, frame_dir, subtitle_path, audio_path
            )
            return {
                "status": "COMPLETED",
                "progress": 100,
                "videoPath": str(video_path),
                "subtitlePath": str(subtitle_path) if subtitle_path else None,
                "audioPath": str(audio_path) if audio_path else None,
                "tokenUsage": len(narration_text),
                "renderSeconds": max(1, len(steps) * 3),
                "concurrencyUnits": 1,
                "errorMessage": None,
            }
        except Exception as exc:
            return {
                "status": "FAILED",
                "progress": 100,
                "videoPath": "",
                "subtitlePath": None,
                "audioPath": None,
                "tokenUsage": 0,
                "renderSeconds": 0,
                "concurrencyUnits": 1,
                "errorMessage": str(exc),
            }
