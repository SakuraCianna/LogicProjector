from pathlib import Path
import subprocess


class VideoCompositor:
    def __init__(self, timeout_seconds: int = 180) -> None:
        self.timeout_seconds = timeout_seconds

    def compose(
        self,
        export_task_id: int,
        frame_dir: Path,
        subtitle_path: Path | None,
        audio_path: Path | None,
    ) -> tuple[Path, list[str]]:
        video_path = frame_dir.parent / f"{export_task_id}.mp4"
        frame_pattern = frame_dir / "frame-%04d.png"
        command = [
            "ffmpeg",
            "-y",
            "-framerate",
            "1/3",
            "-i",
            str(frame_pattern),
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
        command.extend(["-c:v", "libx264", "-pix_fmt", "yuv420p"])
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
