from pathlib import Path


class TtsService:
    def __init__(self, output_root: Path) -> None:
        self.output_root = output_root

    def build_audio_file(
        self, export_task_id: int, summary: str, steps: list[dict]
    ) -> Path:
        audio_path = self.output_root / str(export_task_id) / f"{export_task_id}.mp3"
        audio_path.parent.mkdir(parents=True, exist_ok=True)
        narration_text = summary + "\n" + "\n".join(step["narration"] for step in steps)
        audio_path.write_bytes(narration_text.encode("utf-8"))
        return audio_path
