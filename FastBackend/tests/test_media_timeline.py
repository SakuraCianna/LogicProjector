from pathlib import Path

from PIL import Image

from app.services.frame_renderer import FrameRenderer
from app.services.export_pipeline import ExportPipeline
from app.services.subtitle_service import SubtitleService
from app.services.tts_service import TtsService
from app.services.tts_service import TimelineEntry
from app.services.video_compositor import VideoCompositor


def test_tts_service_builds_step_audio_timeline(tmp_path: Path):
    service = TtsService(tmp_path)
    saved_texts: list[tuple[Path, str]] = []

    def fake_save_audio(audio_path: Path, narration_text: str) -> None:
        saved_texts.append((audio_path, narration_text))
        audio_path.parent.mkdir(parents=True, exist_ok=True)
        audio_path.write_bytes(b"mp3")

    def fake_probe_duration(audio_path: Path) -> float:
        return 1.25 if audio_path.name == "step-0001.mp3" else 2.5

    service._save_audio = fake_save_audio
    service._probe_duration = fake_probe_duration
    service._concatenate_audio_files = lambda clip_paths, output_path: output_path.write_bytes(b"joined")

    audio_path, timeline = service.build_audio_file(
        7,
        "排序开始",
        [
            {"narration": "比较第一个元素"},
            {"narration": "交换两个元素"},
        ],
    )

    assert audio_path == tmp_path / "7" / "7.mp3"
    assert audio_path.read_bytes() == b"joined"
    assert [entry.duration_seconds for entry in timeline] == [1.25, 2.5]
    assert [entry.narration for entry in timeline] == ["比较第一个元素", "交换两个元素"]
    assert [path.name for path, _text in saved_texts] == ["step-0001.mp3", "step-0002.mp3"]


def test_subtitle_service_uses_timeline_durations(tmp_path: Path):
    service = SubtitleService(tmp_path)
    _audio_path, timeline = TtsService(tmp_path).build_silent_timeline(
        [
            {"narration": "比较第一个元素"},
            {"narration": "交换两个元素"},
        ],
        [1.25, 2.5],
    )

    subtitle_path = service.build_subtitle_file(7, timeline)

    assert subtitle_path.read_text(encoding="utf-8") == (
        "1\n"
        "00:00:00,000 --> 00:00:01,250\n"
        "比较第一个元素\n\n"
        "2\n"
        "00:00:01,250 --> 00:00:03,750\n"
        "交换两个元素\n"
    )


def test_subtitle_service_uses_same_minimum_duration_as_frame_timeline(tmp_path: Path):
    service = SubtitleService(tmp_path)

    subtitle_path = service.build_subtitle_file(7, [TimelineEntry(1, "短句", 0.2)])

    assert subtitle_path.read_text(encoding="utf-8") == (
        "1\n"
        "00:00:00,000 --> 00:00:00,500\n"
        "短句\n"
    )


def test_video_compositor_writes_concat_frame_durations(tmp_path: Path):
    frame_dir = tmp_path / "7" / "frames"
    frame_dir.mkdir(parents=True)
    (frame_dir / "frame-0001.png").write_bytes(b"one")
    (frame_dir / "frame-0002.png").write_bytes(b"two")
    compositor = VideoCompositor()
    timeline = [
        TimelineEntry(1, "比较第一个元素", 1.25),
        TimelineEntry(2, "交换两个元素", 2.5),
    ]

    frame_list_path = compositor._write_frame_list(frame_dir, timeline)

    frame_list = frame_list_path.read_text(encoding="utf-8")
    assert "duration 1.250" in frame_list
    assert "duration 2.500" in frame_list
    assert frame_list.count("file ") == 3


def test_video_compositor_normalizes_short_frame_durations(tmp_path: Path):
    frame_dir = tmp_path / "7" / "frames"
    frame_dir.mkdir(parents=True)
    (frame_dir / "frame-0001.png").write_bytes(b"one")
    compositor = VideoCompositor()

    frame_list_path = compositor._write_frame_list(frame_dir, [TimelineEntry(1, "短句", 0.2)])

    assert "duration 0.500" in frame_list_path.read_text(encoding="utf-8")


def test_video_compositor_does_not_mix_vfr_vsync_with_output_frame_rate(tmp_path: Path, monkeypatch):
    frame_dir = tmp_path / "7" / "frames"
    frame_dir.mkdir(parents=True)
    (frame_dir / "frame-0001.png").write_bytes(b"one")
    subtitle_path = tmp_path / "7" / "7.srt"
    subtitle_path.write_text("1\n00:00:00,000 --> 00:00:01,000\n比较\n", encoding="utf-8")
    audio_path = tmp_path / "7" / "7.mp3"
    audio_path.write_bytes(b"mp3")
    captured_command: list[str] = []

    def fake_run(command, **_kwargs):
        captured_command.extend(command)

    monkeypatch.setattr("app.services.video_compositor.subprocess.run", fake_run)
    compositor = VideoCompositor()

    compositor.compose(
        7,
        frame_dir,
        subtitle_path,
        audio_path,
        [TimelineEntry(1, "比较", 1.0)],
    )

    assert "-r" in captured_command
    assert "30" in captured_command
    assert "-vsync" not in captured_command


def test_frame_renderer_outputs_1080p_frames(tmp_path: Path):
    renderer = FrameRenderer(tmp_path)
    payload = {
        "exportTaskId": 7,
        "sourceCode": "public class Demo {}",
        "visualizationPayload": {
            "algorithm": "BUBBLE_SORT",
            "steps": [
                {
                    "title": "比较元素",
                    "narration": "比较索引1和2的两个相邻元素。",
                    "arrayState": [1, 2, 3],
                    "activeIndices": [1, 2],
                    "highlightedLines": [1],
                }
            ],
        },
    }

    frame_dir = renderer.render_frames(payload)

    with Image.open(frame_dir / "frame-0001.png") as image:
        assert image.size == (1920, 1080)


def test_frame_renderer_localizes_export_frame_text(tmp_path: Path):
    renderer = FrameRenderer(tmp_path)

    assert renderer._display_algorithm("QUICK_SORT") == "快速排序"
    assert renderer._display_step_title("Choose pivot") == "选择基准"
    assert renderer._display_step_title("Compare 0 and 1") == "比较元素"
    assert renderer._label("code_focus") == "源码定位"
    assert renderer._display_step_title({"title": "Choose pivot", "displayTitle": "选择主元"}) == "选择主元"


def test_export_pipeline_returns_warning_when_tts_falls_back_to_silent(tmp_path: Path, monkeypatch):
    pipeline = ExportPipeline(tmp_path)
    monkeypatch.setattr(pipeline.frame_renderer, "render_frames", lambda _payload: tmp_path / "9" / "frames")
    monkeypatch.setattr(pipeline.tts_service, "build_silent_timeline", lambda steps: (None, [TimelineEntry(1, "讲解", 1.0)]))

    def fail_tts(*_args):
        raise RuntimeError("tts down")

    monkeypatch.setattr(pipeline.tts_service, "build_audio_file", fail_tts)
    monkeypatch.setattr(pipeline.subtitle_service, "build_subtitle_file", lambda export_task_id, timeline: tmp_path / "9" / "9.srt")
    monkeypatch.setattr(pipeline.video_compositor, "compose", lambda export_task_id, frame_dir, subtitle_path, audio_path, timeline: (tmp_path / "9" / "9.mp4", []))

    result = pipeline.build_export({
        "exportTaskId": 9,
        "summary": "摘要",
        "sourceCode": "class Demo {}",
        "ttsEnabled": True,
        "subtitleEnabled": True,
        "visualizationPayload": {
            "algorithm": "QUICK_SORT",
            "steps": [{"title": "Choose pivot", "narration": "讲解", "arrayState": [1], "activeIndices": [], "highlightedLines": []}],
        },
    })

    assert result["status"] == "COMPLETED"
    assert result["warnings"] == ["TTS_FAILED_FALLBACK_TO_SILENT"]


def test_frame_renderer_centers_code_window_around_highlight(tmp_path: Path):
    renderer = FrameRenderer(tmp_path)
    source_code = "\n".join(f"line {line_number}" for line_number in range(1, 22))

    visible_lines = renderer._visible_code_lines(source_code, [15])

    assert visible_lines[0][0] == 8
    assert visible_lines[-1][0] == 21
    assert (15, "line 15") in visible_lines


def test_frame_renderer_keeps_bars_compact(tmp_path: Path):
    renderer = FrameRenderer(tmp_path)

    positions = renderer._bar_positions([1, 4, 5, 2, 8])

    widths = [width for _left, width in positions]
    left_edges = [left for left, _width in positions]

    assert max(widths) <= 72
    assert min(left_edges) >= 92
    assert positions[-1][0] + positions[-1][1] <= 704
