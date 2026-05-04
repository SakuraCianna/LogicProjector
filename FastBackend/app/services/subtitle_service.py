from pathlib import Path

from app.services.tts_service import TimelineEntry
from app.services.tts_service import timeline_duration_seconds


class SubtitleService:
    def __init__(self, output_root: Path) -> None:
        self.output_root = output_root

    def build_subtitle_file(self, export_task_id: int, timeline: list[TimelineEntry]) -> Path:
        subtitle_path = self.output_root / str(export_task_id) / f"{export_task_id}.srt"
        subtitle_path.parent.mkdir(parents=True, exist_ok=True)

        lines: list[str] = []
        current_second = 0.0
        for index, entry in enumerate(timeline, start=1):
            start = self._format_timestamp(current_second)
            current_second += timeline_duration_seconds(entry)
            end = self._format_timestamp(current_second)
            lines.extend([str(index), f"{start} --> {end}", entry.narration, ""])

        subtitle_path.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")
        return subtitle_path

    def _format_timestamp(self, total_seconds: float) -> str:
        total_milliseconds = int(round(total_seconds * 1000))
        milliseconds = total_milliseconds % 1000
        total_whole_seconds = total_milliseconds // 1000
        seconds = total_whole_seconds % 60
        total_minutes = total_whole_seconds // 60
        minutes = total_minutes % 60
        hours = total_minutes // 60
        return f"{hours:02d}:{minutes:02d}:{seconds:02d},{milliseconds:03d}"
