import shutil
import subprocess
from pathlib import Path

import pytest

from app.services.export_pipeline import ExportPipeline


pytestmark = pytest.mark.skipif(
    shutil.which("ffmpeg") is None, reason="ffmpeg is required"
)


def _create_silent_mp3(audio_path: Path, _narration_text: str | None = None) -> None:
    subprocess.run(
        [
            "ffmpeg",
            "-y",
            "-f",
            "lavfi",
            "-i",
            "anullsrc=r=24000:cl=mono",
            "-t",
            "1",
            str(audio_path),
        ],
        check=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )


def test_builds_completed_export_result(tmp_path, monkeypatch) -> None:
    payload = {
        "exportTaskId": 42,
        "generationTaskId": 7,
        "algorithm": "QUICK_SORT",
        "summary": "Quick sort picks a pivot and partitions the array.",
        "visualizationPayload": {
            "algorithm": "QUICK_SORT",
            "steps": [
                {
                    "title": "Choose pivot",
                    "narration": "Pick the last value as pivot",
                    "arrayState": [5, 1, 4],
                    "activeIndices": [0, 2],
                }
            ],
        },
        "sourceCode": "public class QuickSort {}",
        "subtitleEnabled": True,
        "ttsEnabled": True,
    }

    pipeline = ExportPipeline(output_root=tmp_path)
    monkeypatch.setattr(
        pipeline.tts_service, "_save_audio", _create_silent_mp3, raising=False
    )
    result = pipeline.build_export(payload)

    assert result["status"] == "COMPLETED"
    assert result["videoPath"].endswith("42.mp4")
    assert result["subtitlePath"].endswith("42.srt")
    assert result["audioPath"].endswith("42.mp3")
    assert result["tokenUsage"] > 0
    assert result["renderSeconds"] >= 1
    assert result["concurrencyUnits"] == 1
    assert Path(result["videoPath"]).exists()
    assert Path(result["videoPath"]).stat().st_size > 0
