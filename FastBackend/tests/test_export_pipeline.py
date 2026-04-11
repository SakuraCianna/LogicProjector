from app.services.export_pipeline import ExportPipeline


def test_builds_export_job_with_subtitles_and_optional_tts():
    payload = {
        "task_id": 42,
        "summary": "Quick sort picks a pivot and partitions the array.",
        "steps": [
            {
                "title": "Choose pivot",
                "narration": "Pick the last value as pivot",
                "arrayState": [5, 1, 4],
            }
        ],
        "subtitle_enabled": True,
        "tts_enabled": True,
    }

    pipeline = ExportPipeline()
    job = pipeline.build_job(payload)

    assert job["task_id"] == 42
    assert job["subtitle_path"].endswith(".srt")
    assert job["audio_path"].endswith(".mp3")
    assert job["ffmpeg_command"][0] == "ffmpeg"
