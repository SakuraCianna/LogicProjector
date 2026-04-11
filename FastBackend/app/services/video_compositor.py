class VideoCompositor:
    def build_command(
        self, task_id: int, subtitle_path: str, audio_path: str | None
    ) -> list[str]:
        command = ["ffmpeg", "-i", f"outputs/{task_id}.mp4"]
        if audio_path:
            command.extend(["-i", audio_path])
        command.append(subtitle_path)
        return command
