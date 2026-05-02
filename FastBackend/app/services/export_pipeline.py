import logging
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
            audio_path = None
            if payload.get("ttsEnabled"):
                try:
                    audio_path = self.tts_service.build_audio_file(
                        export_task_id, payload["summary"], steps
                    )
                except Exception as tts_exc:
                    logger.warning("TTS failed for export %s, continuing without audio: %s", export_task_id, tts_exc)
            video_path, _command = self.video_compositor.compose(
                export_task_id, frame_dir, subtitle_path, audio_path
            )
            # 返回相对于 output_root 的路径，便于 Spring 拼接
            video_path_relative = video_path.relative_to(self.output_root)
            subtitle_path_relative = subtitle_path.relative_to(self.output_root) if subtitle_path else None
            audio_path_relative = audio_path.relative_to(self.output_root) if audio_path else None
            return {
                "status": "COMPLETED",
                "progress": 100,
                "videoPath": str(video_path_relative),
                "subtitlePath": str(subtitle_path_relative) if subtitle_path_relative else None,
                "audioPath": str(audio_path_relative) if audio_path_relative else None,
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
