from pathlib import Path

import asyncio

import edge_tts


class TtsService:
    def __init__(
        self, output_root: Path, voice: str = "en-US-AvaMultilingualNeural"
    ) -> None:
        self.output_root = output_root
        self.voice = voice

    def build_audio_file(
        self, export_task_id: int, summary: str, steps: list[dict]
    ) -> Path:
        audio_path = self.output_root / str(export_task_id) / f"{export_task_id}.mp3"
        audio_path.parent.mkdir(parents=True, exist_ok=True)
        narration_text = summary + "\n" + "\n".join(step["narration"] for step in steps)
        self._save_audio(audio_path, narration_text)
        return audio_path

    def _save_audio(self, audio_path: Path, narration_text: str) -> None:
        asyncio.run(self._synthesize(audio_path, narration_text))

    async def _synthesize(self, audio_path: Path, narration_text: str) -> None:
        communicator = edge_tts.Communicate(narration_text, self.voice)
        await communicator.save(str(audio_path))
