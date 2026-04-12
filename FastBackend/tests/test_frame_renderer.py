from pathlib import Path

from app.services.frame_renderer import FrameRenderer


def test_renders_png_frames_for_steps(tmp_path: Path) -> None:
    renderer = FrameRenderer(output_root=tmp_path)
    payload = {
        "exportTaskId": 101,
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

    frame_dir = renderer.render_frames(payload)

    assert (frame_dir / "frame-0001.png").exists()
