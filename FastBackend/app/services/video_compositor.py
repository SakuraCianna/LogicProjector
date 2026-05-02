from pathlib import Path
import subprocess

from app.services.tts_service import TimelineEntry


class VideoCompositor:
    def __init__(self, timeout_seconds: int = 180) -> None:
        self.timeout_seconds = timeout_seconds

    def compose(
        self,
        export_task_id: int,
        frame_dir: Path,
        subtitle_path: Path | None,
        audio_path: Path | None,
        timeline: list[TimelineEntry],
    ) -> tuple[Path, list[str]]:
        video_path = frame_dir.parent / f"{export_task_id}.mp4"
        frame_list_path = self._write_frame_list(frame_dir, timeline)
        command = [
            "ffmpeg",
            "-y",
            "-f",
            "concat",
            "-safe",
            "0",
            "-i",
            str(frame_list_path),
        ]
        if audio_path is not None:
            command.extend(["-i", str(audio_path)])
        if subtitle_path is not None:
            escaped_subtitle_path = (
                subtitle_path.resolve()
                .as_posix()
                .replace(":", "\\:")
                .replace("'", "\\'")
            )
            command.extend(["-vf", f"subtitles='{escaped_subtitle_path}'"])
        command.extend(["-vsync", "vfr", "-r", "30", "-c:v", "libx264", "-preset", "slow", "-crf", "18", "-pix_fmt", "yuv420p"])
        if audio_path is not None:
            command.extend(["-c:a", "aac", "-shortest"])
        else:
            command.append("-an")
        command.append(str(video_path))
        subprocess.run(
            command,
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            timeout=self.timeout_seconds,
        )
        return video_path, command

    def _write_frame_list(self, frame_dir: Path, timeline: list[TimelineEntry]) -> Path:
        frame_list_path = frame_dir.parent / "frames.txt"
        lines: list[str] = []
        last_frame_path: Path | None = None
        for entry in timeline:
            frame_path = frame_dir / f"frame-{entry.step_index:04d}.png"
            last_frame_path = frame_path
            lines.append(f"file '{self._ffmpeg_path(frame_path)}'")
            lines.append(f"duration {max(0.5, entry.duration_seconds):.3f}")
        if last_frame_path is not None:
            lines.append(f"file '{self._ffmpeg_path(last_frame_path)}'")
        frame_list_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
        return frame_list_path

    def _ffmpeg_path(self, path: Path) -> str:
        return path.resolve().as_posix().replace("'", "'\\''")
