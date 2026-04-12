from pathlib import Path


class VideoCompositor:
    def compose(
        self,
        export_task_id: int,
        frame_dir: Path,
        subtitle_path: Path | None,
        audio_path: Path | None,
    ) -> tuple[Path, list[str]]:
        video_path = frame_dir.parent / f"{export_task_id}.mp4"
        command = [
            "ffmpeg",
            "-y",
            "-framerate",
            "1",
            "-i",
            str(frame_dir / "frame-%04d.png"),
        ]
        if audio_path is not None:
            command.extend(["-i", str(audio_path)])
        if subtitle_path is not None:
            command.extend(["-vf", f"subtitles={subtitle_path.as_posix()}"])
        command.append(str(video_path))
        video_path.touch()
        return video_path, command
