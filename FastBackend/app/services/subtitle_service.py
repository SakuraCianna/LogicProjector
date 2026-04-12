from pathlib import Path


class SubtitleService:
    def __init__(self, output_root: Path) -> None:
        self.output_root = output_root

    def build_subtitle_file(self, export_task_id: int, steps: list[dict]) -> Path:
        subtitle_path = self.output_root / str(export_task_id) / f"{export_task_id}.srt"
        subtitle_path.parent.mkdir(parents=True, exist_ok=True)

        lines: list[str] = []
        current_second = 0
        for index, step in enumerate(steps, start=1):
            start = f"00:00:{current_second:02d},000"
            end = f"00:00:{current_second + 3:02d},000"
            lines.extend([str(index), f"{start} --> {end}", step["narration"], ""])
            current_second += 3

        subtitle_path.write_text("\n".join(lines), encoding="utf-8")
        return subtitle_path
