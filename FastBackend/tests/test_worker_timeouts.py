from pathlib import Path
import subprocess

from app.services.tts_service import TtsService
from app.services.video_compositor import VideoCompositor


def test_tts_service_applies_timeout(monkeypatch, tmp_path: Path) -> None:
    observed: dict[str, float] = {}
    service = TtsService(output_root=tmp_path, timeout_seconds=12)

    async def fake_wait_for(coro, timeout):
        observed["timeout"] = timeout
        await coro

    async def fake_synthesize(_audio_path: Path, _narration_text: str) -> None:
        return None

    monkeypatch.setattr("app.services.tts_service.asyncio.wait_for", fake_wait_for)
    monkeypatch.setattr(service, "_synthesize", fake_synthesize)

    service.build_audio_file(7, "summary", [{"narration": "step"}])

    assert observed["timeout"] == 12


def test_video_compositor_passes_timeout_to_ffmpeg(monkeypatch, tmp_path: Path) -> None:
    observed: dict[str, float] = {}
    compositor = VideoCompositor(timeout_seconds=45)
    frame_dir = tmp_path / "42" / "frames"
    frame_dir.mkdir(parents=True)

    def fake_run(command, check, stdout, stderr, timeout):
        observed["timeout"] = timeout
        return subprocess.CompletedProcess(command, 0)

    monkeypatch.setattr("app.services.video_compositor.subprocess.run", fake_run)

    video_path, _command = compositor.compose(42, frame_dir, None, None)

    assert observed["timeout"] == 45
    assert video_path.name == "42.mp4"
