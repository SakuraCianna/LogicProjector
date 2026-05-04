import asyncio
import subprocess
from dataclasses import dataclass
from pathlib import Path

import edge_tts


@dataclass(frozen=True)
class TimelineEntry:
    step_index: int
    narration: str
    duration_seconds: float
    audio_path: Path | None = None


def timeline_duration_seconds(entry: TimelineEntry) -> float:
    return max(0.5, entry.duration_seconds)


class TtsService:
    def __init__(
        self,
        output_root: Path,
        voice: str = "zh-CN-XiaoxiaoNeural",
        rate: str = "+8%",
        pitch: str = "+2Hz",
        timeout_seconds: int = 60,
    ) -> None:
        self.output_root = output_root
        self.voice = voice
        self.rate = rate
        self.pitch = pitch
        self.timeout_seconds = timeout_seconds

    def build_audio_file(
        self, export_task_id: int, summary: str, steps: list[dict]
    ) -> tuple[Path, list[TimelineEntry]]:
        audio_path = self.output_root / str(export_task_id) / f"{export_task_id}.mp3"
        audio_path.parent.mkdir(parents=True, exist_ok=True)
        clip_paths: list[Path] = []
        timeline: list[TimelineEntry] = []

        for index, step in enumerate(steps, start=1):
            narration_text = step.get("narration") or step.get("title") or summary
            clip_path = audio_path.parent / f"step-{index:04d}.mp3"
            self._save_audio(clip_path, narration_text)
            duration_seconds = self._probe_duration(clip_path)
            clip_paths.append(clip_path)
            timeline.append(TimelineEntry(index, narration_text, duration_seconds, clip_path))

        if clip_paths:
            self._concatenate_audio_files(clip_paths, audio_path)

        return audio_path, timeline

    def build_silent_timeline(
        self,
        steps: list[dict],
        durations: list[float] | None = None,
        default_duration_seconds: float = 3.0,
    ) -> tuple[None, list[TimelineEntry]]:
        timeline: list[TimelineEntry] = []
        for index, step in enumerate(steps, start=1):
            duration_seconds = durations[index - 1] if durations and index <= len(durations) else default_duration_seconds
            narration_text = step.get("narration") or step.get("title") or ""
            timeline.append(TimelineEntry(index, narration_text, duration_seconds))
        return None, timeline

    def _save_audio(self, audio_path: Path, narration_text: str) -> None:
        asyncio.run(asyncio.wait_for(self._synthesize(audio_path, narration_text), timeout=self.timeout_seconds))

    async def _synthesize(self, audio_path: Path, narration_text: str) -> None:
        communicator = edge_tts.Communicate(narration_text, self.voice, rate=self.rate, pitch=self.pitch)
        await communicator.save(str(audio_path))

    def _probe_duration(self, audio_path: Path) -> float:
        command = [
            "ffprobe",
            "-v",
            "error",
            "-show_entries",
            "format=duration",
            "-of",
            "default=noprint_wrappers=1:nokey=1",
            str(audio_path),
        ]
        result = subprocess.run(command, check=True, capture_output=True, text=True, timeout=15)
        duration_seconds = float(result.stdout.strip())
        return max(0.5, duration_seconds)

    def _concatenate_audio_files(self, clip_paths: list[Path], output_path: Path) -> None:
        concat_path = output_path.parent / "audio-concat.txt"
        concat_path.write_text(
            "\n".join(f"file '{clip_path.resolve().as_posix().replace(chr(39), chr(39) + chr(92) + chr(39) + chr(39))}'" for clip_path in clip_paths),
            encoding="utf-8",
        )
        command = [
            "ffmpeg",
            "-y",
            "-f",
            "concat",
            "-safe",
            "0",
            "-i",
            str(concat_path),
            "-c:a",
            "libmp3lame",
            "-q:a",
            "4",
            str(output_path),
        ]
        subprocess.run(command, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=self.timeout_seconds)
