from app.services.export_pipeline import ExportPipeline


def test_builds_completed_export_result(tmp_path) -> None:
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
    result = pipeline.build_export(payload)

    assert result["status"] == "COMPLETED"
    assert result["videoPath"].endswith("42.mp4")
    assert result["subtitlePath"].endswith("42.srt")
    assert result["audioPath"].endswith("42.mp3")
    assert result["tokenUsage"] > 0
    assert result["renderSeconds"] >= 1
    assert result["concurrencyUnits"] == 1
