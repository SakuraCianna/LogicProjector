import logging
import math
import os
from pathlib import Path

from app.services.frame_renderer import FrameRenderer
from app.services.subtitle_service import SubtitleService
from app.services.tts_service import TtsService
from app.services.video_compositor import VideoCompositor

logger = logging.getLogger(__name__)


class ExportPipeline:
    def __init__(self, output_root: Path | None = None) -> None:
        self.output_root = output_root or Path(os.environ.get("PAS_OUTPUT_ROOT", "outputs"))
        self.frame_renderer = FrameRenderer(self.output_root)
        self.subtitle_service = SubtitleService(self.output_root)
        self.tts_service = TtsService(self.output_root)
        self.video_compositor = VideoCompositor()

    def build_export(self, payload: dict) -> dict:
        export_task_id = payload["exportTaskId"]
        summary = payload["summary"]
        steps = payload["visualizationPayload"]["steps"]
        narration_text = summary + " " + " ".join(step["narration"] for step in steps)
        try:
            frame_dir = self.frame_renderer.render_frames(payload)
            audio_path = None
            _silent_audio_path, timeline = self.tts_service.build_silent_timeline(steps)
            if payload.get("ttsEnabled"):
                try:
                    audio_path, timeline = self.tts_service.build_audio_file(
                        export_task_id, summary, steps
                    )
                except Exception as tts_exc:
                    logger.warning("TTS failed for export %s, continuing without audio: %s", export_task_id, tts_exc)
            subtitle_path = (
                self.subtitle_service.build_subtitle_file(export_task_id, timeline)
                if payload.get("subtitleEnabled")
                else None
            )
            video_path, _command = self.video_compositor.compose(
                export_task_id, frame_dir, subtitle_path, audio_path, timeline
            )
            video_path_relative = str(video_path.relative_to(self.output_root))
            subtitle_path_relative = self._relative_output_path(subtitle_path)
            audio_path_relative = self._relative_output_path(audio_path)
            render_seconds = max(1, math.ceil(sum(entry.duration_seconds for entry in timeline)))
            return self._success_result(
                video_path=video_path_relative,
                subtitle_path=subtitle_path_relative,
                audio_path=audio_path_relative,
                token_usage=len(narration_text),
                render_seconds=render_seconds,
            )
        except Exception as exc:
            return self._failure_result(str(exc))

    def _relative_output_path(self, path: Path | None) -> str | None:
        return str(path.relative_to(self.output_root)) if path else None

    def _success_result(
        self,
        *,
        video_path: str,
        subtitle_path: str | None,
        audio_path: str | None,
        token_usage: int,
        render_seconds: int,
    ) -> dict:
        return {
            "status": "COMPLETED",
            "progress": 100,
            "videoPath": video_path,
            "subtitlePath": subtitle_path,
            "audioPath": audio_path,
            "tokenUsage": token_usage,
            "renderSeconds": render_seconds,
            "concurrencyUnits": 1,
            "errorMessage": None,
        }

    def _failure_result(self, error_message: str) -> dict:
        return {
            "status": "FAILED",
            "progress": 100,
            "videoPath": "",
            "subtitlePath": None,
            "audioPath": None,
            "tokenUsage": 0,
            "renderSeconds": 0,
            "concurrencyUnits": 1,
            "errorMessage": error_message,
        }
