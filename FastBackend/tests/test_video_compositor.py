import shutil
import subprocess
from pathlib import Path

import pytest

from app.services.frame_renderer import FrameRenderer
from app.services.video_compositor import VideoCompositor


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


def test_composes_non_empty_mp4_from_frames_subtitles_and_audio(tmp_path: Path) -> None:
    renderer = FrameRenderer(output_root=tmp_path)
    frame_dir = renderer.render_frames(
        {
            "exportTaskId": 77,
            "visualizationPayload": {
                "algorithm": "QUICK_SORT",
                "steps": [
                    {
                        "title": "Compare",
                        "narration": "Compare left and pivot",
                        "arrayState": [5, 1, 4],
                        "activeIndices": [0, 1],
                    }
                ],
            },
        }
    )

    subtitle_path = tmp_path / "77" / "77.srt"
    subtitle_path.write_text(
        "1\n00:00:00,000 --> 00:00:01,000\nCompare left and pivot\n", encoding="utf-8"
    )

    audio_path = tmp_path / "77" / "77.mp3"
    _create_silent_mp3(audio_path)

    video_path, command = VideoCompositor().compose(
        77, frame_dir, subtitle_path, audio_path
    )

    assert command[0] == "ffmpeg"
    assert video_path.exists()
    assert video_path.stat().st_size > 0
